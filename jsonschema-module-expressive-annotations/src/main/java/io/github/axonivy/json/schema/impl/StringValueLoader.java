package io.github.axonivy.json.schema.impl;

import java.util.Optional;
import java.util.logging.Level;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Load plain string to native target types: bool, array[string], ...
 */
public class StringValueLoader {

  private final String field;
  private final JavaType fieldType;
  private final ObjectMapper mapper;

  public StringValueLoader(String field, JavaType fieldType, ObjectMapper mapper) {
    this.field = field;
    this.fieldType = fieldType;
    this.mapper = mapper;
  }

  public JsonNode valueOf(String input) {
    var stringDefault = JsonNodeFactory.instance.stringNode(input);
    if (fieldType.getRawClass() == String.class) {
      return stringDefault;
    }

    try {
      Object value = readValue(input, fieldType);
      return mapper.valueToTree(value);
    } catch (JacksonException | IllegalArgumentException ex) {
      JacksonDefaultValueProvider.LOGGER.log(Level.WARNING,
          "Could not parse default value for field '" + field + "' as " + fieldType.getTypeName()
              + "; using its string value (" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + ")");
      return stringDefault;
    }
  }

  private Object readValue(String input, JavaType targetType) {
    try {
      var reader = mapper.reader().forType(targetType);
      return reader.readValue(input);
    } catch (JacksonException ex) {
      if (targetType.getRawClass().isEnum()) {
        try {
          return mapper.convertValue(input, targetType);
        } catch (IllegalArgumentException enumEx) {
          throw invalidDefault(input, targetType, enumEx);
        }
      }
      throw invalidDefault(input, targetType, ex);
    }
  }

  private IllegalArgumentException invalidDefault(String defaultValue, JavaType targetType, Throwable cause) {
    return new IllegalArgumentException(
        "Default value for field '" + field + "' is not valid JSON for type '" + targetType.getTypeName()
            + "': " + defaultValue,
        cause);
  }

  public JsonNode valueCollectionAware(String input) {
    var stringExample = JsonNodeFactory.instance.stringNode(input);
    if (fieldType.getRawClass() == String.class) {
      return stringExample;
    }

    Optional<JsonNode> parsed = tryReadValue(input, fieldType);
    if (parsed.isPresent()) {
      return parsed.get();
    }
    if (fieldType.isCollectionLikeType() || fieldType.isArrayType()) {
      JavaType itemType = fieldType.getContentType();
      if (itemType != null) {
        parsed = tryReadValue(input, itemType);
        if (parsed.isPresent()) {
          return parsed.get();
        }
      }
    }
    return stringExample;
  }

  private Optional<JsonNode> tryReadValue(String input, JavaType targetType) {
    try {
      return Optional.of(mapper.valueToTree(readValue(input, targetType)));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }
}
