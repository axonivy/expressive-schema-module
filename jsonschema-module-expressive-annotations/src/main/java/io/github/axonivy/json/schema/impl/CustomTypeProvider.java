package io.github.axonivy.json.schema.impl;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.AnnotationHelper;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import io.github.axonivy.json.schema.annotations.CustomType;

public class CustomTypeProvider implements CustomPropertyDefinitionProvider<FieldScope>, CustomDefinitionProviderV2 {

  private static final Predicate<Annotation> CONSIDER_NESTED = a -> !a.toString().startsWith("@java.lang.annotation");

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    var reference = Optional.ofNullable(scope.getAnnotation(CustomType.class, CONSIDER_NESTED));
    if (reference.isEmpty()) {
      return null;
    }

    ResolvedType resolvedType = context.getTypeContext().resolve(reference.get().value());
    var definition = context.createDefinitionReference(resolvedType);
    return new CustomPropertyDefinition(definition);
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    var reference = AnnotationHelper.resolveAnnotation(javaType.getErasedType(), CustomType.class, CONSIDER_NESTED);
    if (reference.isEmpty()) {
      return null;
    }
    ResolvedType resolvedType = context.getTypeContext().resolve(reference.get().value());
    var definition = context.createStandardDefinition(resolvedType, this);
    return new CustomDefinition(definition);
  }

}
