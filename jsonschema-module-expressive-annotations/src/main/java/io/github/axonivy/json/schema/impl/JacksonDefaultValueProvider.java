package io.github.axonivy.json.schema.impl;

import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.FieldScope;

public class JacksonDefaultValueProvider implements ConfigFunction<FieldScope, Object> {

  @Override
  public Object apply(FieldScope field) {
    var annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
    return Optional.ofNullable(annotation)
        .map(JsonProperty::defaultValue)
        .filter(Predicate.not(String::isEmpty))
        .orElse(null);
  }

}
