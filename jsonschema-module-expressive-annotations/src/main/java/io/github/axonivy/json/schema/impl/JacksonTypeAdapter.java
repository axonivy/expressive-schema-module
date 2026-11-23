package io.github.axonivy.json.schema.impl;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import tools.jackson.databind.JavaType;

final class JacksonTypeAdapter {

  private JacksonTypeAdapter() {
  }

  static JavaType toJavaType(FieldScope field, SchemaGenerationContext context) {
    return toJavaType(CustomTypeProvider.resolveEffectiveTypeFor(field, context), context);
  }

  static JavaType toJavaType(ResolvedType resolvedType, SchemaGenerationContext context) {
    var type = CustomTypeProvider.resolveCustomTypeFor(resolvedType, context).orElse(resolvedType);
    var mapper = context.getGeneratorConfig().getObjectMapper();
    var typeFactory = mapper.getTypeFactory();

    if (type.isArray()) {
      return typeFactory.constructArrayType(toJavaType(type.getArrayElementType(), context));
    }

    var parameters = type.getTypeBindings().getTypeParameters();
    if (parameters.isEmpty()) {
      return mapper.constructType(type.getErasedType());
    }

    JavaType[] javaParameters = parameters.stream()
        .map(parameter -> toJavaType(parameter, context))
        .toArray(JavaType[]::new);
    return typeFactory.constructParametricType(type.getErasedType(), javaParameters);
  }
}
