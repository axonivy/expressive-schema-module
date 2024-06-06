package io.github.axonivy.json.schema.impl;

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

import io.github.axonivy.json.schema.annotations.Condition;

public class ConditionalFieldProvider implements CustomDefinitionProviderV2 {

  private DynamicRefs refs;

  public ConditionalFieldProvider(DynamicRefs refs) {
    this.refs = refs;
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    ResolvedTypeWithMembers fully = context.getTypeContext().resolveWithMembers(javaType);
    var builder = new ConditionBuilder(context).refs(refs);
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
    private DynamicRefs refs = new DynamicRefs();

    public ConditionBuilder(SchemaGenerationContext context) {
      this.version = context.getGeneratorConfig().getSchemaVersion();
    }

    public ConditionBuilder refs(DynamicRefs resolver) {
      this.refs = resolver;
      return this;
    }

    public ObjectNode addCondition(Condition condition, String field) {
      var resovled = refs.resolve(condition.thenRef());
      var ref = JsonNodeFactory.instance.objectNode();
      ref.put(SchemaKeyword.TAG_REF.forVersion(version), resovled);
      return addCondition(field, condition.ifConst(), condition.thenProperty(), ref);
    }

    public ObjectNode addCondition(String field, String expect, String thenProp, JsonNode thenRef) {
      return addCondition(field, new String[] {expect}, thenProp, thenRef);
    }

    public ObjectNode addCondition(String field, String[] expect, String thenProp, JsonNode thenRef) {
      var target = JsonNodeFactory.instance.objectNode();
      ifProperty(field, expect, target);
      thenProperty(thenProp, thenRef, target);
      conditions.add(target);
      return target;
    }

    private void ifProperty(String field, String[] expect, ObjectNode target) {
      var ifProperty = target
        .putObject(SchemaKeyword.TAG_IF.forVersion(version))
        .putObject(SchemaKeyword.TAG_PROPERTIES.forVersion(version))
        .putObject(field);

      String constTag = SchemaKeyword.TAG_CONST.forVersion(version);
      if (expect.length == 1) {
        ifProperty.put(constTag, expect[0]);
        return;
      }
      var anyOf = ifProperty.putArray(SchemaKeyword.TAG_ANYOF.forVersion(version));
      for(String exp : expect) {
        anyOf.addObject().put(constTag, exp);
      }
    }

    private void thenProperty(String thenProp, JsonNode thenRef, ObjectNode target) {
      target
        .putObject(SchemaKeyword.TAG_THEN.forVersion(version))
        .putObject(SchemaKeyword.TAG_PROPERTIES.forVersion(version))
        .set(thenProp, thenRef);
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