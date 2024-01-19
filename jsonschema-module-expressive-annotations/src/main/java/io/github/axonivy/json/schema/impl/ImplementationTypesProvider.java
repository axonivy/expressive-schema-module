package io.github.axonivy.json.schema.impl;

import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_ENUM;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE_STRING;

import java.lang.reflect.Constructor;
import java.util.Optional;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.AllImplementations;
import io.github.axonivy.json.schema.annotations.AllImplementations.TypeReqistry;
import io.github.axonivy.json.schema.impl.ConditionalFieldProvider.ConditionBuilder;


/**
 * Specify valid implementing types of a parent interface.
 */
public class ImplementationTypesProvider implements CustomDefinitionProviderV2 {

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType type, SchemaGenerationContext context) {
    var impls = type.getErasedType().getAnnotation(AllImplementations.class);
    if (impls == null) {
      return null;
    }

    TypeReqistry registry = create(impls.value());
    var base = Optional.ofNullable(registry.base())
      .map(context.getTypeContext()::resolve)
      .orElse(type);
    ObjectNode std = context.createStandardDefinition(base, this);

    var props = propertiesOf(context, std);
    props.set(impls.type(), craftEnum(context, registry));
    props.putObject(impls.container()).put("type", "object"); //vs-code needs a generic block for validation

    return conditional(context, impls, registry)
      .toDefinition(()->std)
      .map(node -> new CustomDefinition(node, false))
      .orElse(null);
  }

  private static ObjectNode propertiesOf(SchemaGenerationContext context, ObjectNode std) {
    var version = context.getGeneratorConfig().getSchemaVersion();
    String propertiesTag = SchemaKeyword.TAG_PROPERTIES.forVersion(version);
    if (std.get(propertiesTag) instanceof ObjectNode props) {
      return props;
    }
    return std.putObject(propertiesTag);
  }

  private static ConditionBuilder conditional(SchemaGenerationContext context, AllImplementations impls, TypeReqistry registry) {
    var builder = new io.github.axonivy.json.schema.impl.ConditionalFieldProvider.ConditionBuilder(context);
    for(Class<?> subType : registry.types()) {
      var resolved = context.getTypeContext().resolve(subType);
      ObjectNode reference = context.createDefinitionReference(resolved);
      builder.addCondition(impls.type(), registry.typeName(subType), impls.container(), reference);
    }
    return builder;
  }

  private static TypeReqistry create(Class<? extends TypeReqistry> registryType) {
    try {
      Constructor<?> constructor = registryType.getConstructors()[0];
      return (TypeReqistry) constructor.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to create type registry", ex);
    }
  }

  private static ObjectNode craftEnum(SchemaGenerationContext context, TypeReqistry registry) {
    ObjectNode typeDef = context.getGeneratorConfig().createObjectNode();
    var version = context.getGeneratorConfig().getSchemaVersion();
    typeDef.put(TAG_TYPE.forVersion(version), TAG_TYPE_STRING.forVersion(version));
    ArrayNode items = typeDef.putArray(TAG_ENUM.forVersion(version));
    registry.types().stream()
      .map(registry::typeName)
      .sorted()
      .forEach(items::add);
    return typeDef;
  }

}
