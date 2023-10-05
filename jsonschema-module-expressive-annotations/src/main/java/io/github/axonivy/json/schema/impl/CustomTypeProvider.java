package io.github.axonivy.json.schema.impl;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import io.github.axonivy.json.schema.annotations.CustomType;

public class CustomTypeProvider implements CustomPropertyDefinitionProvider<FieldScope> {

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    CustomType reference = scope.getAnnotation(CustomType.class);
    if (reference == null) {
      return null;
    }

    ResolvedType resolvedType = context.getTypeContext().resolve(reference.value());
    ObjectNode definition = context.createDefinitionReference(resolvedType);
    return new CustomPropertyDefinition(definition);
  }
}
