package io.github.rew.json.schema.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.classmate.Annotations;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.github.rew.json.schema.annotations.Condition;

public class ConditionalFieldProvider implements CustomDefinitionProviderV2 {

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    ResolvedTypeWithMembers fully = context.getTypeContext().resolveWithMembers(javaType);
    var builder = new ConditionBuilder(context);
    Arrays.stream(fully.getMemberFields())
      .forEach(fld -> annotate(builder, fld));
    return builder
      .toDefinition(()->context.createStandardDefinition(javaType, this))
      .map(CustomDefinition::new)
      .orElse(null);
  }

  private void annotate(ConditionBuilder builder, ResolvedField field) {
    Consumer<Condition> add = condition -> builder.addCondition(condition, field.getName());
    Annotations annotations = field.getAnnotations();
    Condition single = field.get(Condition.class);
    if (single != null) {
      add.accept(single);
    }
    Condition.List conditions = annotations.get(Condition.List.class);
    if (conditions != null) {
      Arrays.stream(conditions.value()).forEachOrdered(add);
    }
  }

  public static class ConditionBuilder {

    private final SchemaVersion version;
    private final List<ObjectNode> conditions = new ArrayList<>();

    public ConditionBuilder(SchemaGenerationContext context) {
      this.version = context.getGeneratorConfig().getSchemaVersion();
    }

    public ObjectNode addCondition(Condition condition, String field) {
      var resovled = DynamicRefs.resolve(condition.thenRef());
      var ref = JsonNodeFactory.instance.objectNode();
      ref.put(SchemaKeyword.TAG_REF.forVersion(version), resovled);
      return addCondition(field, condition.ifConst(), condition.thenProperty(), ref);
    }

    public ObjectNode addCondition(String field, String expect, String thenProp, JsonNode thenRef) {
      var target = JsonNodeFactory.instance.objectNode();
      target.putObject("if").putObject("properties")
        .putObject(field)
        .put("const", expect);
      target.putObject("then").putObject("properties")
        .set(thenProp, thenRef);
      conditions.add(target);
      return target;
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