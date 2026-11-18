package io.github.axonivy.json.schema.impl;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    return findMapKeyPattern(type, Map.of());
  }

  private static Pattern findMapKeyPattern(AnnotatedType type, Map<TypeVariable<?>, AnnotatedType> substitutions) {
    if (type == null) {
      return null;
    }
    Class<?> rawType = getRawType(type);
    if (rawType == null) {
      return null;
    }

    Map<TypeVariable<?>, AnnotatedType> currentSubstitutions = new HashMap<>(substitutions);
    if (type instanceof AnnotatedParameterizedType parameterizedType) {
      AnnotatedType[] typeArguments = parameterizedType.getAnnotatedActualTypeArguments();
      TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
      for (int index = 0; index < Math.min(typeParameters.length, typeArguments.length); index++) {
        currentSubstitutions.put(typeParameters[index], typeArguments[index]);
      }
    }

    if (rawType == Map.class && type instanceof AnnotatedParameterizedType parameterizedType) {
      AnnotatedType[] typeArguments = parameterizedType.getAnnotatedActualTypeArguments();
      if (typeArguments.length > 0) {
        Pattern pattern = findPattern(typeArguments[0], currentSubstitutions, new HashSet<>());
        if (pattern != null) {
          return pattern;
        }
      }
    }
    Pattern pattern = findMapKeyPattern(rawType.getAnnotatedSuperclass(), currentSubstitutions);
    if (pattern != null) {
      return pattern;
    }
    for (AnnotatedType interfaceType : rawType.getAnnotatedInterfaces()) {
      pattern = findMapKeyPattern(interfaceType, currentSubstitutions);
      if (pattern != null) {
        return pattern;
      }
    }
    return null;
  }

  private static Pattern findPattern(AnnotatedType type, Map<TypeVariable<?>, AnnotatedType> substitutions,
      Set<TypeVariable<?>> resolving) {
    if (type == null) {
      return null;
    }
    Pattern pattern = type.getAnnotation(Pattern.class);
    if (pattern != null) {
      return pattern;
    }
    if (type.getType() instanceof TypeVariable<?> typeVariable && resolving.add(typeVariable)) {
      return findPattern(substitutions.get(typeVariable), substitutions, resolving);
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
