package io.github.axonivy.json.schema.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Predicate;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.victools.jsonschema.generator.AnnotationHelper;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class EnumDescriptionProvider implements CustomDefinitionProviderV2 {

  private static final Predicate<Annotation> CONSIDER_NESTED = annotation ->
      annotation.annotationType().isAnnotationPresent(JacksonAnnotationsInside.class);

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    Class<?> enumType = javaType.getErasedType();
    if (!enumType.isEnum()) {
      return null;
    }

    Object[] enumConstants = enumType.getEnumConstants();
    JsonPropertyDescription[] descriptions = new JsonPropertyDescription[enumConstants.length];
    boolean hasDescriptions = false;
    for (int index = 0; index < enumConstants.length; index++) {
      descriptions[index] = getDescription(enumType, (Enum<?>) enumConstants[index]);
      hasDescriptions |= descriptions[index] != null;
    }
    if (!hasDescriptions) {
      return null;
    }

    ObjectNode definition = context.createStandardDefinition(javaType, this);
    String enumKeyword = context.getKeyword(SchemaKeyword.TAG_ENUM);
    JsonNode enumValues = definition.remove(enumKeyword);
    if (!(enumValues instanceof ArrayNode values) || values.size() != enumConstants.length) {
      throw new IllegalStateException("Cannot add enum constant descriptions to " + enumType.getName()
          + ": the standard definition does not contain one value per enum constant.");
    }

    ArrayNode oneOf = definition.putArray(context.getKeyword(SchemaKeyword.TAG_ONEOF));
    for (int index = 0; index < enumConstants.length; index++) {
      ObjectNode enumValue = oneOf.addObject();
      enumValue.set(context.getKeyword(SchemaKeyword.TAG_CONST), values.get(index));
      if (descriptions[index] != null) {
        enumValue.put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), descriptions[index].value());
      }
    }
    return new CustomDefinition(definition);
  }

  private static JsonPropertyDescription getDescription(Class<?> enumType, Enum<?> enumConstant) {
    try {
      Field enumField = enumType.getField(enumConstant.name());
      return AnnotationHelper.resolveAnnotation(enumField, JsonPropertyDescription.class, CONSIDER_NESTED).orElse(null);
    } catch (NoSuchFieldException exception) {
      throw new IllegalStateException("Missing enum constant field " + enumType.getName() + "." + enumConstant.name(), exception);
    }
  }
}
