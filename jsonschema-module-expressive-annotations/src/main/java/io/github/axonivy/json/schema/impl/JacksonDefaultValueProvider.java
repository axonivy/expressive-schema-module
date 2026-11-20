package io.github.axonivy.json.schema.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class JacksonDefaultValueProvider implements InstanceAttributeOverrideV2<FieldScope> {

  private static final Logger LOGGER = Logger.getLogger(JacksonDefaultValueProvider.class.getName());

  @Override
  public void overrideInstanceAttributes(ObjectNode node, FieldScope field, SchemaGenerationContext context) {
    if (field.isFakeContainerItemScope()) {
      return;
    }
    JsonProperty annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
    if (annotation == null || annotation.defaultValue().isEmpty()) {
      return;
    }

    var defaultVal = valueOf(field, context, annotation);
    node.set(context.getKeyword(SchemaKeyword.TAG_DEFAULT), defaultVal);
  }

  private JsonNode valueOf(FieldScope field, SchemaGenerationContext context, JsonProperty annotation) {
    String defaultValue = annotation.defaultValue();
    var fieldType = field.getType().getErasedType();
    var objectMapper = context.getGeneratorConfig().getObjectMapper();

    var defaultNode = objectMapper.valueToTree(defaultValue);
    if (fieldType == String.class) {
      return defaultNode;
    }

    try {
      Object value = readValue(field, fieldType, defaultValue, objectMapper);
      return objectMapper.valueToTree(value);
    } catch (JacksonException | IllegalArgumentException ex) {
      LOGGER.log(Level.WARNING,
          "Could not parse default value for field '" + field.getName() + "' as " + fieldType.getTypeName()
              + "; using its string value (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
      return defaultNode;
    }
  }

  private static Object readValue(FieldScope field, Class<?> fieldType, String defaultValue, ObjectMapper objectMapper) {
    try {
      return objectMapper.reader().forType(fieldType).readValue(defaultValue);
    } catch (JacksonException ex) {
      if (fieldType.isEnum()) {
        try {
          return objectMapper.convertValue(defaultValue, fieldType);
        } catch (IllegalArgumentException enumEx) {
          throw invalidDefault(field, fieldType, defaultValue, enumEx);
        }
      }
      throw invalidDefault(field, fieldType, defaultValue, ex);
    }
  }

  private static IllegalArgumentException invalidDefault(FieldScope field, Class<?> fieldType, String defaultValue,
      Throwable cause) {
    return new IllegalArgumentException(
        "Default value for field '" + field.getName() + "' is not valid JSON for type '" + fieldType.getTypeName()
            + "': " + defaultValue,
        cause);
  }

}
