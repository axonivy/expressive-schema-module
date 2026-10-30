package io.github.axonivy.json.schema.tests;

import static io.github.axonivy.json.schema.tests.TestImplementationTypes.namesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.axonivy.json.schema.annotations.Condition;
import io.github.axonivy.json.schema.annotations.Conditional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

class TestFieldCondition {

  @Test
  void ifThenElse() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyConditionalField.class);

    JsonNode ifConst = schema.get("if").get("properties").get("provider").get("const");
    assertThat(ifConst.asString())
        .isEqualTo("azure-idp");
    assertThat(schema.get("if").get("required")).isNull();

    var then = schema.get("then").get("properties").get("config").get("$ref");
    assertThat(then.asString())
        .isEqualTo("/ivy/azure-config.json");
  }

  static class MyConditionalField {
    @Condition(ifConst = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
    public String provider;
  }

  @Test
  void multiple_ifThenElse() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyMultiConditionalField.class);

    var allOf = (ArrayNode) schema.get("allOf");
    List<JsonNode> conditions = new ArrayList<>(allOf.values());
    assertThat(conditions).hasSize(2);

    JsonNode first = allOf.get(0);
    JsonNode ifConst = first.get("if").get("properties").get("provider").get("const");
    assertThat(ifConst.asString())
        .isEqualTo("azure-idp");

    var then = first.get("then").get("properties").get("config").get("$ref");
    assertThat(then.asString())
        .isEqualTo("/ivy/azure-config.json");
  }

  static class MyMultiConditionalField {
    @Condition(ifConst = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
    @Condition(ifConst = "ms-ad", thenProperty = "config", thenRef = "/ivy/ldap-config.json")
    public String provider;
  }

  @Test
  void repeatedConditionsCanTargetDifferentProperties() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyDifferentTargetProperties.class);

    var allOf = (ArrayNode) schema.get("allOf");
    assertThat(allOf).hasSize(3);
    assertThat(allOf.get(0).get("then").get("properties").get("config").get("$ref").asString())
        .isEqualTo("/ivy/azure-config.json");
    assertThat(allOf.get(1).get("if").get("not").get("required").get(0).asString())
        .isEqualTo("provider");
    assertThat(allOf.get(1).get("then").get("properties").get("config").get("$ref").asString())
        .isEqualTo("/ivy/azure-config.json");
    assertThat(allOf.get(2).get("then").get("properties").get("settings").get("$ref").asString())
        .isEqualTo("/ivy/ldap-config.json");
  }

  static class MyDifferentTargetProperties {
    @JsonProperty(defaultValue = "azure-idp")
    @Condition(ifConst = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
    @Condition(ifConst = "ms-ad", thenProperty = "settings", thenRef = "/ivy/ldap-config.json")
    public String provider;
  }

  @Test
  void missingProperty_fallsBackToDefaultCondition() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyMissingCondition.class);

    var allOf = (ArrayNode) schema.get("allOf");
    assertThat(allOf).hasSize(3);

    JsonNode local = allOf.get(0);
    assertThat(local.get("if").get("required").get(0).asString()).isEqualTo("Name");
    assertThat(local.get("if").get("properties").get("Name").get("const").asString()).isEqualTo("local");
    assertThat(local.get("then").get("properties").get("Config").get("$ref").asString())
        .isEqualTo("/ivy/blob-storage-local.json");

    JsonNode missingName = allOf.get(1);
    assertThat(missingName.get("if").get("not").get("required").get(0).asString()).isEqualTo("Name");
    assertThat(missingName.get("then").get("properties").get("Config").get("$ref").asString())
      .isEqualTo("/ivy/blob-storage-local.json");

    JsonNode s3 = allOf.get(2);
    assertThat(s3.get("if").get("required").get(0).asString()).isEqualTo("Name");
    assertThat(s3.get("if").get("properties").get("Name").get("const").asString()).isEqualTo("s3");
  }

  static class MyMissingCondition {
    @JsonProperty(defaultValue = "local")
    @Condition(ifConst = "local", thenProperty = "Config", thenRef = "/ivy/blob-storage-local.json")
    @Condition(ifConst = "s3", thenProperty = "Config", thenRef = "/ivy/blob-storage-s3.json")
    public String Name;
  }

  @Test
  void multiple_constsToVerfiy() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyAnyOfConditionalField.class);

    JsonNode provider = schema.get("if").get("properties").get("provider");
    var anyOf = provider.get("anyOf");
    assertThat(anyOf).isInstanceOf(ArrayNode.class).hasSize(2);

    JsonNode ifConst = anyOf.get(0).get("const");
    assertThat(ifConst.asString())
        .isEqualTo("ms-ad");

    var then = schema.get("then").get("properties").get("config").get("$ref");
    assertThat(then.asString())
        .isEqualTo("#/$defs/ComplexType");
  }

  static class MyAnyOfConditionalField {
    public String $schema; // self-ref

    @Condition(ifConst = {"ms-ad", "azure-idp"}, thenProperty = "config", thenRef = "#/$defs/ComplexType")
    public String provider;

    public ComplexType always;

    public static class ComplexType {
      public String name;
    }
  }

  @Test
  void conditionalOtherProp() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyConditionalFieldSibling.class);

    assertThat(namesOf(schema.get("properties")))
        .as("conditional fields are not listed as classic 'properties'")
        .containsOnly("$schema", "provider");

    JsonNode ifProvider = schema.get("if").get("properties").get("provider");
    assertThat(ifProvider.get("const").asString())
        .isEqualTo("azure");

    JsonNode thenProperty = schema.get("then").get("properties").get("ifAzure");
    assertThat(thenProperty.get("$ref").asString())
        .isEqualTo("#/$defs/ComplexType");
  }

  static class MyConditionalFieldSibling {
    public String $schema; // self-ref

    public String provider;

    @Conditional(ifProperty = "provider", hasConst = {"azure"})
    private ComplexType ifAzure;

    public static class ComplexType {
      public String name;
    }
  }

  @Test
  void conditionAndConditionalOtherProp() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyMixedCondition.class);

    var allOf = (ArrayNode) schema.get("allOf");
    assertThat(allOf)
        .as("multiple kinds of conditionals as one 'allOf' condition")
        .isInstanceOf(ArrayNode.class);

    JsonNode first = allOf.get(0);
    JsonNode ifProvider = first.get("if").get("properties").get("provider");
    assertThat(ifProvider.get("const").asString())
        .isEqualTo("ms-ad");
    JsonNode thenProperty = first.get("then").get("properties").get("ifAd");
    assertThat(thenProperty.get("$ref").asString())
        .isEqualTo("#/$defs/ComplexType");

    JsonNode second = allOf.get(1);
    JsonNode ifOther = second.get("if").get("properties").get("provider");
    assertThat(ifOther.get("const").asString())
        .isEqualTo("azure");
    JsonNode thenThis = second.get("then").get("properties").get("ifAzure");
    assertThat(thenThis.get("$ref").asString())
        .isEqualTo("#/$defs/AzureType");
  }

  static class MyMixedCondition {
    public String $schema; // self-ref

    @Condition(ifConst = {"ms-ad"}, thenProperty = "ifAd", thenRef = "#/$defs/ComplexType")
    public String provider;

    @Conditional(ifProperty = "provider", hasConst = {"azure"})
    public AzureType ifAzure;

    public static class AzureType {
      public String id;
    }

    public ComplexType always;

    public static class ComplexType {
      public String name;
    }
  }

}
