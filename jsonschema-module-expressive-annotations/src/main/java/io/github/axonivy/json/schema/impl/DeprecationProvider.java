package io.github.axonivy.json.schema.impl;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.InstanceAttributeOverrideV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import tools.jackson.databind.node.ObjectNode;

public class DeprecationProvider implements InstanceAttributeOverrideV2<FieldScope> {

  @Override
  public void overrideInstanceAttributes(ObjectNode node, FieldScope field, SchemaGenerationContext context) {
    if (field.isFakeContainerItemScope()) {
      return;
    }
    if (field.getAnnotationConsideringFieldAndGetter(Deprecated.class) != null) {
      node.put("deprecated", true);
    }
  }
}
