package io.github.axonivy.json.schema.impl;

import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import tools.jackson.databind.node.ObjectNode;

public class JacksonDefaultValueProvider implements InstanceAttributeOverrideV2<FieldScope> {

  static final Logger LOGGER = Logger.getLogger(JacksonDefaultValueProvider.class.getName());

  @Override
  public void overrideInstanceAttributes(ObjectNode node, FieldScope field, SchemaGenerationContext context) {
    if (field.isFakeContainerItemScope()) {
      return;
    }
    JsonProperty annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
    if (annotation == null || annotation.defaultValue().isEmpty()) {
      return;
    }

    var loader = new StringValueLoader(field.getName(),
        field.getType().getErasedType(),
        context.getGeneratorConfig().getObjectMapper());
    var defaultVal = loader.valueOf(annotation.defaultValue());
    node.set(context.getKeyword(SchemaKeyword.TAG_DEFAULT), defaultVal);
  }

}
