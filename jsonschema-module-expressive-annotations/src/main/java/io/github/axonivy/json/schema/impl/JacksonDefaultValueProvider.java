package io.github.axonivy.json.schema.impl;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class JacksonDefaultValueProvider
    implements ConfigFunction<FieldScope, Object>, InstanceAttributeOverrideV2<FieldScope> {

  @Override
  public Object apply(FieldScope field) {
    if (field.isFakeContainerItemScope() || isArrayType(field)) {
      return null;
    }
    return defaultValue(field);
  }

  private static String defaultValue(FieldScope field) {
    var annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
    if (annotation == null || annotation.defaultValue().isEmpty()) {
      return null;
    }
    return annotation.defaultValue();
  }

  private static boolean isArrayType(FieldScope field) {
    Class<?> type = field.getType().getErasedType();
    return type.isArray() || Collection.class.isAssignableFrom(type);
  }

  @Override
  public void overrideInstanceAttributes(ObjectNode node, FieldScope field, SchemaGenerationContext context) {
    if (field.isFakeContainerItemScope() || !isArrayType(field)) {
      return;
    }
    String defaultValue = defaultValue(field);
    if (defaultValue == null) {
      return;
    }
    var objectMapper = context.getGeneratorConfig().getObjectMapper();
    node.set(context.getKeyword(SchemaKeyword.TAG_DEFAULT), parseArrayDefault(field, defaultValue, objectMapper));
  }

  private static ArrayNode parseArrayDefault(FieldScope field, String defaultValue, ObjectMapper objectMapper) {
    String message = "Default value for array field '" + field.getName() + "' must be a JSON array: " + defaultValue;
    try {
      JsonNode parsed = objectMapper.readTree(defaultValue);
      if (!(parsed instanceof ArrayNode array)) {
        throw new IllegalArgumentException(message);
      }
      return array;
    } catch (JacksonException ex) {
      throw new IllegalArgumentException(message, ex);
    }
  }

}
