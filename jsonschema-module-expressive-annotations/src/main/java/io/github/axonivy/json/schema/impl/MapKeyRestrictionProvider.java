package io.github.axonivy.json.schema.impl;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
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
    AnnotatedType declaredType = field.getRawMember().getAnnotatedType();
    if (!(declaredType instanceof AnnotatedParameterizedType parameterizedType)) {
      return;
    }
    AnnotatedType[] typeArguments = parameterizedType.getAnnotatedActualTypeArguments();
    if (typeArguments.length == 0) {
      return;
    }
    Pattern pattern = typeArguments[0].getAnnotation(Pattern.class);
    if (pattern == null) {
      return;
    }
    ObjectNode propertyNames = node.putObject("propertyNames");
    propertyNames.put(context.getKeyword(SchemaKeyword.TAG_PATTERN), pattern.regexp());
  }
}
