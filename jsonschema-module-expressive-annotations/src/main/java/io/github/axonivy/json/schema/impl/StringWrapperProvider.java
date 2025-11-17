package io.github.axonivy.json.schema.impl;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import io.github.axonivy.json.schema.annotations.StringWrapper;

public class StringWrapperProvider implements CustomDefinitionProviderV2 {

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    var wrapper = javaType.getErasedType().getAnnotation(StringWrapper.class);
    if (wrapper == null) {
      return null;
    }
    ResolvedType string = context.getTypeContext().resolve(String.class);
    var def = context.createStandardDefinitionReference(string, this);
    return new CustomDefinition(def);
  }

}
