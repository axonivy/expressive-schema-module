package io.github.axonivy.json.schema.impl;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import io.github.axonivy.json.schema.annotations.CustomType;

public class CustomTypeProvider implements CustomPropertyDefinitionProvider<FieldScope>, CustomDefinitionProviderV2 {

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    CustomType reference = scope.getAnnotation(CustomType.class);
    if (reference == null) {
      return null;
    }

    ResolvedType resolvedType = context.getTypeContext().resolve(reference.value());
    var definition = context.createDefinitionReference(resolvedType);
    return new CustomPropertyDefinition(definition);
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    CustomType reference = javaType.getErasedType().getAnnotation(CustomType.class);
    if (reference == null) {
      return null;
    }
    ResolvedType resolvedType = context.getTypeContext().resolve(reference.value());
    var definition = context.createStandardDefinition(resolvedType, this);
    return new CustomDefinition(definition);
  }

}
