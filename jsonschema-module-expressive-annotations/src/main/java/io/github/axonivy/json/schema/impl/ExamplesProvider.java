package io.github.axonivy.json.schema.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.classmate.ResolvedType;
import com.github.victools.jsonschema.generator.AnnotationHelper;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.CustomType;
import io.github.axonivy.json.schema.annotations.Examples;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
    declareExamples(def, examples, scope.getName(), JacksonTypeAdapter.toJavaType(scope.getType(), context), context);
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

  private static void declareExamples(ObjectNode def, Examples examples, String name, JavaType type,
      SchemaGenerationContext context) {
    JsonNode allOf = def.get(SchemaKeyword.TAG_ALLOF.forVersion(context.getGeneratorConfig().getSchemaVersion()));
    var loader = new StringValueLoader(name, type, context.getGeneratorConfig().getObjectMapper());
    if (allOf instanceof ArrayNode holder) {
      declare(examples, holder.addObject(), loader);
    } else {
      declare(examples, def, loader);
    }
  }

  private static void declare(Examples examples, ObjectNode holder, StringValueLoader loader) {
    var jExamples = holder.putArray(Key.EXAMPLES);
    Arrays.stream(examples.value()).map(loader::valueCollectionAware).forEachOrdered(jExamples::add);
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
    ResolvedType definitionType = AnnotationHelper
        .resolveAnnotation(javaType.getErasedType(), CustomType.class,
            annotation -> !annotation.toString().startsWith("@java.lang.annotation"))
        .map(customType -> context.getTypeContext().resolve(customType.value()))
        .orElse(javaType);
    var def = context.createStandardDefinition(definitionType, this);
    declareExamples(def, examples, javaType.getErasedType().getSimpleName(),
        JacksonTypeAdapter.toJavaType(definitionType, context), context);
    return new CustomDefinition(def);
  }

}
