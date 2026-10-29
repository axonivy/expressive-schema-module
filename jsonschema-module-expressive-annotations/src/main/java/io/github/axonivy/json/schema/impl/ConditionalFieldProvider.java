package io.github.axonivy.json.schema.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.github.axonivy.json.schema.annotations.Condition;
import io.github.axonivy.json.schema.annotations.Conditional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public class ConditionalFieldProvider implements CustomDefinitionProviderV2 {

  private final DynamicRefs refs;

  public ConditionalFieldProvider(DynamicRefs refs) {
    this.refs = refs;
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    ResolvedTypeWithMembers fully = context.getTypeContext().resolveWithMembers(javaType);
    var builder = new ConditionBuilder(context).refs(refs);
    Arrays.stream(fully.getMemberFields())
        .forEach(fld -> annotate(builder, fld, context));
    return builder
        .toDefinition(() -> context.createStandardDefinition(javaType, this))
        .map(CustomDefinition::new)
        .orElse(null);
  }

  private void annotate(ConditionBuilder builder, ResolvedField field, SchemaGenerationContext context) {
    Annotations annotations = field.getAnnotations();
    JsonProperty jsonProperty = annotations.get(JsonProperty.class);
    String defaultValue = jsonProperty == null || jsonProperty.defaultValue().isEmpty()
        ? null
        : jsonProperty.defaultValue();

    var fieldConditions = readConditions(field, annotations);
    if (!fieldConditions.isEmpty()) {
      Map<String, List<Condition>> conditionsByProperty = new LinkedHashMap<>();
      for (Condition fieldCondition : fieldConditions) {
        conditionsByProperty.computeIfAbsent(fieldCondition.thenProperty(), ignored -> new ArrayList<>())
            .add(fieldCondition);
      }
      conditionsByProperty.forEach((thenProperty, propertyConditions) -> {
        var rule = builder.field(field.getName(), thenProperty, defaultValue);
        propertyConditions.forEach(valueCondition ->
            rule.whenValue(valueCondition.ifConst(), valueCondition.thenRef()));
        rule.add();
      });
    }

    Consumer<Conditional> addConditional = c -> builder.addConditional(c, field,
        () -> context.createStandardDefinitionReference(field.getType(), ConditionalFieldProvider.this));
    Conditional conditional = field.get(Conditional.class);
    if (conditional != null) {
      addConditional.accept(conditional);
    }
    Conditional.List conditionals = annotations.get(Conditional.List.class);
    if (conditionals != null) {
      Arrays.stream(conditionals.value()).forEachOrdered(addConditional);
    }
  }

  private static List<Condition> readConditions(ResolvedField field, Annotations annotations) {
    List<Condition> fieldConditions = new ArrayList<>();
    Condition condition = field.get(Condition.class);
    if (condition != null) {
      fieldConditions.add(condition);
    }
    var conditions = annotations.get(Condition.List.class);
    if (conditions != null) {
      fieldConditions.addAll(Arrays.asList(conditions.value()));
    }
    return fieldConditions;
  }

  public static class ConditionBuilder {

    private final SchemaVersion version;
    private final List<ObjectNode> conditions = new ArrayList<>();
    private final Map<String, List<ObjectNode>> conditionsByField = new HashMap<>();
    private final Set<String> fieldsWithFallback = new HashSet<>();
    private DynamicRefs refs = new DynamicRefs();

    public ConditionBuilder(SchemaGenerationContext context) {
      this.version = context.getGeneratorConfig().getSchemaVersion();
    }

    public ConditionBuilder refs(DynamicRefs resolver) {
      this.refs = resolver;
      return this;
    }

    public FieldBuilder field(String name, String thenProperty) {
      return new FieldBuilder(name, thenProperty, null);
    }

    public FieldBuilder field(String name, String thenProperty, String defaultValue) {
      return new FieldBuilder(name, thenProperty, defaultValue);
    }

    public ObjectNode addConditional(Conditional condition, ResolvedField field, Supplier<ObjectNode> reference) {
      return append(field(condition.ifProperty(), field.getName())
          .whenValue(condition.hasConst(), reference.get()));
    }

    private ObjectNode append(FieldBuilder rule) {
      ObjectNode first = null;
      for (ValueCondition valueCondition : rule.valueConditions) {
        var target = JsonNodeFactory.instance.objectNode();
        ifProperty(rule.name, valueCondition.expectedValues, target);
        thenProperty(rule.thenProperty, valueCondition.schema, target);
        conditions.add(target);
        ObjectNode ifCondition = (ObjectNode) target.get(SchemaKeyword.TAG_IF.forVersion(version));
        if (fieldsWithFallback.contains(rule.name)) {
          requireField(ifCondition, rule.name);
        }
        List<ObjectNode> fieldConditions = conditionsByField.computeIfAbsent(rule.name, ignored -> new ArrayList<>());
        fieldConditions.add(ifCondition);
        if (rule.defaultValue != null && valueCondition.expectedValues.contains(rule.defaultValue)) {
          if (fieldsWithFallback.add(rule.name)) {
            fieldConditions.forEach(ifSchema -> requireField(ifSchema, rule.name));
          }
          addMissingCondition(rule.name, rule.thenProperty, valueCondition.schema);
        }
        if (first == null) {
          first = target;
        }
      }
      return first;
    }

    private ObjectNode reference(String path) {
      var ref = JsonNodeFactory.instance.objectNode();
      ref.put(SchemaKeyword.TAG_REF.forVersion(version), refs.resolve(path));
      return ref;
    }

    private ObjectNode addMissingCondition(String field, String property, JsonNode fallbackSchema) {
      var target = JsonNodeFactory.instance.objectNode();
      target
          .putObject(SchemaKeyword.TAG_IF.forVersion(version))
          .putObject(SchemaKeyword.TAG_NOT.forVersion(version))
          .putArray(SchemaKeyword.TAG_REQUIRED.forVersion(version))
          .add(field);
      thenProperty(property, fallbackSchema, target);
      conditions.add(target);
      return target;
    }

    private void requireField(ObjectNode ifCondition, String field) {
      String propertiesTag = SchemaKeyword.TAG_PROPERTIES.forVersion(version);
      JsonNode properties = ifCondition.remove(propertiesTag);
      ifCondition.putArray(SchemaKeyword.TAG_REQUIRED.forVersion(version)).add(field);
      if (properties != null) {
        ifCondition.set(propertiesTag, properties);
      }
    }

    private void ifProperty(String name, List<String> expectedValues, ObjectNode target) {
      var ifCondition = target.putObject(SchemaKeyword.TAG_IF.forVersion(version));
      var ifProperty = ifCondition
          .putObject(SchemaKeyword.TAG_PROPERTIES.forVersion(version))
          .putObject(name);

      String constTag = SchemaKeyword.TAG_CONST.forVersion(version);
      if (expectedValues.size() == 1) {
        ifProperty.put(constTag, expectedValues.get(0));
        return;
      }
      var anyOf = ifProperty.putArray(SchemaKeyword.TAG_ANYOF.forVersion(version));
      for (String expectedValue : expectedValues) {
        anyOf.addObject().put(constTag, expectedValue);
      }
    }

    private void thenProperty(String property, JsonNode schema, ObjectNode target) {
      target
          .putObject(SchemaKeyword.TAG_THEN.forVersion(version))
          .putObject(SchemaKeyword.TAG_PROPERTIES.forVersion(version))
          .set(property, schema);
    }

    public final class FieldBuilder {

      private final String name;
      private final String thenProperty;
      private final String defaultValue;
      private final List<ValueCondition> valueConditions = new ArrayList<>();

      private FieldBuilder(String name, String thenProperty, String defaultValue) {
        this.name = name;
        this.thenProperty = thenProperty;
        this.defaultValue = defaultValue;
      }

      public FieldBuilder whenValue(String expectedValue, String reference) {
        return whenValue(new String[] {expectedValue}, reference);
      }

      public FieldBuilder whenValue(String expectedValue, JsonNode schema) {
        return whenValue(new String[] {expectedValue}, schema);
      }

      public FieldBuilder whenValue(String[] expectedValues, String reference) {
        return whenValue(expectedValues, ConditionBuilder.this.reference(reference));
      }

      public FieldBuilder whenValue(String[] expectedValues, JsonNode schema) {
        valueConditions.add(new ValueCondition(Arrays.asList(expectedValues), schema));
        return this;
      }

      public ConditionBuilder add() {
        ConditionBuilder.this.append(this);
        return ConditionBuilder.this;
      }
    }

    private static class ValueCondition {

      private final List<String> expectedValues;
      private final JsonNode schema;

      private ValueCondition(List<String> expectedValues, JsonNode schema) {
        this.expectedValues = expectedValues;
        this.schema = schema;
      }
    }

    public Optional<ObjectNode> toDefinition(Supplier<ObjectNode> std) {
      if (conditions.isEmpty()) {
        return Optional.empty();
      }
      ObjectNode def = std.get();
      if (conditions.size() == 1) {
        def.setAll(conditions.get(0));
      } else {
        ArrayNode allOf = def.putArray(SchemaKeyword.TAG_ALLOF.forVersion(version));
        allOf.addAll(conditions);
      }
      return Optional.of(def);
    }
  }

}
