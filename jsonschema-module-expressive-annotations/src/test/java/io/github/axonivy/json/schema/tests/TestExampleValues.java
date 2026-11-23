package io.github.axonivy.json.schema.tests;

import static io.github.axonivy.json.schema.tests.TestTypesAsFields.namesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.github.axonivy.json.schema.annotations.CustomType;
import io.github.axonivy.json.schema.annotations.Examples;
import io.github.axonivy.json.schema.impl.JacksonDefaultValueProvider;
import io.github.axonivy.json.schema.tests.TestExampleValues.ChatModel;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class TestExampleValues {

  @Test
  void enrichedWithExamples() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Examplified.class);
    assertThat(schema.toPrettyString()).contains("superFastProvider");
  }

  static class Examplified {
    @Examples({ "superFastProvider", "speedBuster", "cheapAndSlow" })
    public String provider;
  }

  @Test
  void typesWithExamples() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Container.class);
    var typeRef = schema.get("$defs").get(TypeReference.class.getSimpleName());
    var examples = typeRef.get("examples");
    assertThat(textNodes(examples))
        .containsExactly("com.acme.MyId", "com.acme.MyId:methodRef");
  }

  static class Container {
    TypeReference ref;
  }

  @Examples({ "com.acme.MyId", "com.acme.MyId:methodRef" })
  static class TypeReference {
  }

  @Test
  void listExamples() {
    var logRecords = new LogRecorder();
    var schema = logRecords.record(Logger.getLogger(JacksonDefaultValueProvider.class.getName()).getName(), ()->new ExpressiveSchemaGenerator().generateSchema(ChatModel.class));
    ObjectNode schemaNode = (ObjectNode) schema;
    System.out.println(schemaNode.toPrettyString());

    var capabilities = schema.get("properties").get("capabilities");
    assertThat(textNodes(capabilities.get("examples")))
        .containsOnly("JSON_SCHEMA", "IMAGE_GENERATOR");

    var items = capabilities.get("items");
    assertThat(namesOf(items))
        .as("examples are only stated on the array, not on its items")
        .containsOnly("type");
    assertThat(logRecords.warnings())
        .isEmpty();

    var priorities = schema.get("properties").get("priorities").get("examples");
    assertThat(priorities.get(0).isIntegralNumber()).isTrue();
    assertThat(priorities.get(0).asInt()).isEqualTo(1);
    assertThat(priorities.get(1).isIntegralNumber()).isTrue();
    assertThat(priorities.get(1).asInt()).isEqualTo(2);

    var priorityGroups = schema.get("properties").get("priorityGroups").get("examples");
    assertThat(priorityGroups.get(0).isArray()).isTrue();
    assertThat(priorityGroups.get(0).get(0).asInt()).isEqualTo(3);
  }

  static List<String> textNodes(JsonNode node) {
    if (node instanceof ArrayNode array) {
      List<String> values = new ArrayList<>();
      array.elements().stream().forEach(n -> values.add(n.asString()));
      return values;
    }
    return List.of();
  }

  static class ChatModel {
    @Examples({ "JSON_SCHEMA", "IMAGE_GENERATOR" })
    List<String> capabilities;

    @Examples({ "1", "2" })
    List<Integer> priorities;

    @Examples({ "[3,4]" })
    List<Integer> priorityGroups;
  }

  @Test
  void examplesPreserveNativeAndNestedTypes() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(TypedExamples.class);
    var properties = schema.get("properties");

    var enabled = properties.get("enabled").get("examples");
    assertThat(enabled.get(0).isBoolean()).isTrue();
    assertThat(enabled.get(0).asBoolean()).isTrue();

    var retries = properties.get("retries").get("examples");
    assertThat(retries.get(0).isIntegralNumber()).isTrue();
    assertThat(retries.get(0).asInt()).isEqualTo(3);

    var groupedScores = properties.get("groupedScores").get("examples").get(0);
    assertThat(groupedScores.get("scores").get(0).isIntegralNumber()).isTrue();
    assertThat(groupedScores.get("scores").get(0).asInt()).isEqualTo(1);
  }

  @Test
  void examplesUseFieldCustomType() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(CustomTypeExamples.class);
    var example = schema.get("properties").get("value").get("examples").get(0);

    assertThat(example.isString()).isTrue();
    assertThat(example.asString()).isEqualTo("true");
  }

  static class CustomTypeExamples {
    @CustomType(String.class)
    @Examples({ "true" })
    Object value;
  }

  static class TypedExamples {
    @Examples({ "true", "false" })
    boolean enabled;

    @Examples({ "3", "5" })
    int retries;

    @Examples({ "{\"scores\":[1,2]}" })
    Map<String, List<Integer>> groupedScores;
  }

}
