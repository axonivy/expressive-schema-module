package io.github.axonivy.json.schema.impl;

import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_ANYOF;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_CONST;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_DESCRIPTION;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_ENUM;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE_STRING;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.Implementations;
import io.github.axonivy.json.schema.annotations.Implementations.TypeReqistry;
import io.github.axonivy.json.schema.impl.ConditionalFieldProvider.ConditionBuilder;


/**
 * Specify valid implementing types of a parent interface.
 */
public class ImplementationTypesProvider implements CustomDefinitionProviderV2 {

  private final boolean conditional;

  public ImplementationTypesProvider(boolean conditional) {
    this.conditional = conditional;
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType type, SchemaGenerationContext context) {
    var impls = type.getErasedType().getAnnotation(Implementations.class);
    if (impls == null) {
      return null;
    }

    TypeReqistry registry = create(impls.value());
    var base = Optional.ofNullable(registry.base())
      .map(context.getTypeContext()::resolve)
      .orElse(type);
    ObjectNode std = context.createStandardDefinition(base, this);

    var props = propertiesOf(context, std);
    props.set(impls.type(), craftTypeConsts(context, registry));

    var container = props.putObject(impls.container());
    if (!conditional) {
      container.setAll(craftAnyOf(context, registry.types()));
      return new CustomDefinition(std, false);
    }

    container.put("type", "object"); //vs-code needs a generic block for validation
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

  private static ConditionBuilder conditional(SchemaGenerationContext context, Implementations impls, TypeReqistry registry) {
    var builder = new ConditionBuilder(context);
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
      throw new RuntimeException("Failed to create type registry: "+registryType+". Does it have a public zero-arg constructor?", ex);
    }
  }

  private JsonNode craftTypeConsts(SchemaGenerationContext context, TypeReqistry registry) {
    var described = registry.types().stream()
      .collect(Collectors.toMap(
        type -> registry.typeName(type),
        type -> Optional.ofNullable(registry.typeDesc(type)).orElse(""))
      );
    if (described.values().stream()
        .filter(Predicate.not(String::isBlank))
        .findAny().isEmpty()) {
      return craftEnum(context, described.keySet());
    }
    return enumerateValidIds(context, described);
  }

  public static ObjectNode craftEnum(SchemaGenerationContext context, Collection<String> consts) {
    ObjectNode typeDef = context.getGeneratorConfig().createObjectNode();
    var version = context.getGeneratorConfig().getSchemaVersion();
    typeDef.put(TAG_TYPE.forVersion(version), TAG_TYPE_STRING.forVersion(version));
    ArrayNode items = typeDef.putArray(TAG_ENUM.forVersion(version));
    consts.stream()
      .sorted()
      .forEach(items::add);
    return typeDef;
  }

  public static ObjectNode enumerateValidIds(SchemaGenerationContext context, Map<String, String> described) {
    var version = context.getGeneratorConfig().getSchemaVersion();
    var consts = context.getGeneratorConfig().createObjectNode();
    consts.put(TAG_TYPE.forVersion(version), TAG_TYPE_STRING.forVersion(version));
    var anyOf = consts.putArray(TAG_ANYOF.forVersion(version));
    described.entrySet().stream().forEach(et -> {
      ObjectNode impl = anyOf.addObject();
      impl.put(TAG_CONST.forVersion(version), et.getKey());
      if (!et.getValue().isBlank()) {
        impl.put(TAG_DESCRIPTION.forVersion(version), et.getValue());
      }
    });
    return consts;
  }

  public static ObjectNode craftAnyOf(SchemaGenerationContext context, Set<Class<?>> subTypes) {
    ObjectNode typeDef = context.getGeneratorConfig().createObjectNode();
    var version = context.getGeneratorConfig().getSchemaVersion();
    var anyOf = typeDef.putArray(TAG_ANYOF.forVersion(version));
    subTypes.stream()
      .map(nodeType -> context.createDefinitionReference(context.getTypeContext().resolve(nodeType)))
      .forEachOrdered(anyOf::add);
    return typeDef;
  }

}
