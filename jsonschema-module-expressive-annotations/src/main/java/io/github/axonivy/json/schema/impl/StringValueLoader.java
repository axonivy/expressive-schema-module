package io.github.axonivy.json.schema.impl;

import java.util.logging.Level;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Load plain string to native target types: bool, array[string], ...
 */
public class StringValueLoader {

  private final String field;
  private final Class<?> fieldType;
  private final ObjectMapper mapper;

  public StringValueLoader(String field, Class<?> fieldType, ObjectMapper mapper) {
    this.field = field;
    this.fieldType = fieldType;
    this.mapper = mapper;
  }

  public JsonNode valueOf(String defaultValue) {
    var stringDefault = JsonNodeFactory.instance.stringNode(defaultValue);
    if (defaultValue.contains("{")) {
      return stringDefault;
    }
    if (fieldType == String.class) {
      return stringDefault;
    }

    try {
      Object value = readValue(defaultValue, mapper);
      return mapper.valueToTree(value);
    } catch (JacksonException | IllegalArgumentException ex) {
      JacksonDefaultValueProvider.LOGGER.log(Level.WARNING,
          "Could not parse default value for field '" + field + "' as " + fieldType.getTypeName()
              + "; using its string value (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
      return stringDefault;
    }
  }

  private Object readValue(String defaultValue, ObjectMapper objectMapper) {
    try {
      return objectMapper.reader().forType(fieldType).readValue(defaultValue);
    } catch (JacksonException ex) {
      if (fieldType.isEnum()) {
        try {
          return objectMapper.convertValue(defaultValue, fieldType);
        } catch (IllegalArgumentException enumEx) {
          throw invalidDefault(defaultValue, enumEx);
        }
      }
      throw invalidDefault(defaultValue, ex);
    }
  }

  private IllegalArgumentException invalidDefault(String defaultValue, Throwable cause) {
    return new IllegalArgumentException(
        "Default value for field '" + field + "' is not valid JSON for type '" + fieldType.getTypeName()
            + "': " + defaultValue,
        cause);
  }
}
