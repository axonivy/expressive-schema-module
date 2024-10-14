package io.github.axonivy.json.schema.impl;

import java.lang.reflect.Constructor;
import java.util.Optional;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.TypesAsFields;
import io.github.axonivy.json.schema.annotations.TypesAsFields.FieldRegistry;


public class TypesAsFieldsProvider implements CustomDefinitionProviderV2 {

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    TypesAsFields typesAsFields = javaType.getErasedType().getAnnotation(TypesAsFields.class);
    if (typesAsFields == null) {
      return null;
    }
    ObjectNode std = context.createStandardDefinition(javaType, this);
    String properties = context.getKeyword(SchemaKeyword.TAG_PROPERTIES);
    var props = Optional.ofNullable((ObjectNode)std.get(properties))
            .orElseGet(() -> std.putObject(properties));
    FieldRegistry registry = create(typesAsFields.value());
    new FieldCreator(registry, context).typesAsFields(props);
    return new CustomDefinition(std);
  }

  private static class FieldCreator {

    private final FieldRegistry registry;
    private final SchemaGenerationContext context;

    private FieldCreator(FieldRegistry registry, SchemaGenerationContext context) {
      this.registry = registry;
      this.context = context;
    }

    private void typesAsFields(ObjectNode props) {
      registry.types().stream()
        .sorted((c1,c2) -> String.CASE_INSENSITIVE_ORDER.compare(c1.getSimpleName(), c2.getSimpleName()))
        .forEachOrdered(type -> {
          toProperty(props, type);
      });
    }

    private void toProperty(ObjectNode props, Class<?> type) {
      String name = registry.fieldName(type);
      var refType = registry.valueType(type);
      var inner = context.getTypeContext().resolve(refType);
      var def = context.createDefinitionReference(inner);
      Optional.ofNullable(registry.fieldDescription(type)).ifPresent(desc -> {
        def.put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), desc);
      });
      props.set(name, def);
    }

  }

  private static FieldRegistry create(Class<? extends FieldRegistry> registryType) {
    try {
      Constructor<?> constructor = registryType.getConstructors()[0];
      return (FieldRegistry) constructor.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to create type registry: "+registryType+". Does it have a public zero-arg constructor?", ex);
    }
  }
}
