package io.github.axonivy.json.schema.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.Examples;

public class ExamplesProvider implements CustomPropertyDefinitionProvider<FieldScope>, CustomDefinitionProviderV2 {

  private final Set<Field> visited = new HashSet<>();

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    Examples examples = scope.getAnnotation(Examples.class);
    if (examples == null) {
      return null;
    }
    var def = resolveStd(scope, context);
    if (def == null) {
      return null;
    }
    declareExamples(def, examples, context);
    return new CustomPropertyDefinition(def);
  }

  private ObjectNode resolveStd(FieldScope scope, SchemaGenerationContext context) {
    var unique = scope.getMember().getRawMember();
    if (visited.contains(unique)) {
      return null; // do not re-declare examples for collection-members
    }
    visited.add(unique);
    try {
      return context.createStandardDefinition(scope, this);
    } finally {
      visited.remove(unique);
    }
  }

  private static void declareExamples(ObjectNode def, Examples examples, SchemaGenerationContext context) {
    JsonNode allOf = def.get(SchemaKeyword.TAG_ALLOF.forVersion(context.getGeneratorConfig().getSchemaVersion()));
    if (allOf instanceof ArrayNode holder) {
      declare(examples, holder.addObject());
    } else {
      declare(examples, def);
    }
  }

  private static void declare(Examples examples, ObjectNode holder) {
    var jExamples = holder.putArray(Key.EXAMPLES);
    Arrays.stream(examples.value()).forEachOrdered(jExamples::add);
  }

  private interface Key {
    String EXAMPLES = "examples";
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    var examples = javaType.getErasedType().getAnnotation(Examples.class);
    if (examples == null) {
      return null;
    }
    var def = context.createStandardDefinition(javaType, this);
    declareExamples(def, examples, context);
    return new CustomDefinition(def);
  }

}
