package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.Condition;

class TestFieldCondition {

  @Test
  void ifThenElse() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyConditionalField.class);

    JsonNode ifConst = schema.get("if").get("properties").get("provider").get("const");
    assertThat(ifConst.asText())
      .isEqualTo("azure-idp");

    var then = schema.get("then").get("properties").get("config").get("$ref");
    assertThat(then.asText())
      .isEqualTo("/ivy/azure-config.json");
  }

  static class MyConditionalField {
    @Condition(ifConst  = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
    public String provider;
  }

  @Test
  void multiple_ifThenElse() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyMultiConditionalField.class);

    var allOf = (ArrayNode) schema.get("allOf");
    List<JsonNode> conditions = new ArrayList<>();
    allOf.elements().forEachRemaining(conditions::add);
    assertThat(conditions).hasSize(2);

    JsonNode first = allOf.get(0);
    JsonNode ifConst = first.get("if").get("properties").get("provider").get("const");
    assertThat(ifConst.asText())
      .isEqualTo("azure-idp");

    var then = first.get("then").get("properties").get("config").get("$ref");
    assertThat(then.asText())
      .isEqualTo("/ivy/azure-config.json");
  }

  static class MyMultiConditionalField {
    @Condition(ifConst  = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
    @Condition(ifConst  = "ms-ad", thenProperty = "config", thenRef = "/ivy/ldap-config.json")
    public String provider;
  }

  @Test
  void multiple_constsToVerfiy() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyAnyOfConditionalField.class);
    System.out.println(schema.toPrettyString());

    JsonNode provider = schema.get("if").get("properties").get("provider");
    var anyOf = provider.get("anyOf");
    assertThat(anyOf).isInstanceOf(ArrayNode.class).hasSize(2);

    JsonNode ifConst = anyOf.get(0).get("const");
    assertThat(ifConst.asText())
      .isEqualTo("ms-ad");

    var then = schema.get("then").get("properties").get("config").get("$ref");
    assertThat(then.asText())
      .isEqualTo("#/$defs/ComplexType");
  }

  static class MyAnyOfConditionalField {
    public String $schema; // self-ref

    @Condition(ifConst  = { "ms-ad", "azure-idp" }, thenProperty = "config", thenRef = "#/$defs/ComplexType")
    public String provider;

    public ComplexType always;

    public static class ComplexType {
      public String name;
    }
  }

}
