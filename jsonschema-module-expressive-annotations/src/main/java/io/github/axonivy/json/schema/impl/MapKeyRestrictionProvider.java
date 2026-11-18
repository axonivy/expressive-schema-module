package io.github.axonivy.json.schema.impl;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.validation.constraints.Pattern;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import tools.jackson.databind.node.ObjectNode;

public class MapKeyRestrictionProvider implements InstanceAttributeOverrideV2<FieldScope> {

  @Override
  public void overrideInstanceAttributes(ObjectNode node, FieldScope field, SchemaGenerationContext context) {
    if (!Map.class.isAssignableFrom(field.getType().getErasedType())) {
      return;
    }
    Pattern pattern = findMapKeyPattern(field.getRawMember().getAnnotatedType());
    if (pattern == null) {
      return;
    }
    ObjectNode propertyNames = node.putObject("propertyNames");
    propertyNames.put(context.getKeyword(SchemaKeyword.TAG_PATTERN), pattern.regexp());
  }

  private static Pattern findMapKeyPattern(AnnotatedType type) {
    if (type == null) {
      return null;
    }
    Class<?> rawType = getRawType(type);
    if (rawType == null) {
      return null;
    }
    if (Map.class.isAssignableFrom(rawType) && type instanceof AnnotatedParameterizedType parameterizedType) {
      AnnotatedType[] typeArguments = parameterizedType.getAnnotatedActualTypeArguments();
      if (typeArguments.length > 0) {
        Pattern pattern = typeArguments[0].getAnnotation(Pattern.class);
        if (pattern != null) {
          return pattern;
        }
      }
    }
    Pattern pattern = findMapKeyPattern(rawType.getAnnotatedSuperclass());
    if (pattern != null) {
      return pattern;
    }
    for (AnnotatedType interfaceType : rawType.getAnnotatedInterfaces()) {
      pattern = findMapKeyPattern(interfaceType);
      if (pattern != null) {
        return pattern;
      }
    }
    return null;
  }

  private static Class<?> getRawType(AnnotatedType type) {
    Type javaType = type.getType();
    if (javaType instanceof Class<?> rawType) {
      return rawType;
    }
    if (javaType instanceof ParameterizedType parameterizedType
        && parameterizedType.getRawType() instanceof Class<?> rawType) {
      return rawType;
    }
    return null;
  }
}
