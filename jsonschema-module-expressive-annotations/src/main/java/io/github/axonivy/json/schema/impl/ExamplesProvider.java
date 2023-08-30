package io.github.axonivy.json.schema.impl;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.Examples;

public class ExamplesProvider implements CustomPropertyDefinitionProvider<FieldScope> {

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    Examples examples = scope.getAnnotation(Examples.class);
    if (examples == null) {
      return null;
    }
    var def = context.createStandardDefinition(scope, this);

    JsonNode allOf = def.get(SchemaKeyword.TAG_ALLOF.forVersion(context.getGeneratorConfig().getSchemaVersion()));
    if (allOf instanceof ArrayNode holder) {
      declare(examples, holder.addObject());
    } else {
      declare(examples, def);
    }
    return new CustomPropertyDefinition(def);
  }

  private void declare(Examples examples, ObjectNode holder) {
    var jExamples = holder.putArray(Key.EXAMPLES);
    Arrays.stream(examples.value()).forEachOrdered(jExamples::add);
  }

  private interface Key {
    String EXAMPLES = "examples";
  }

}