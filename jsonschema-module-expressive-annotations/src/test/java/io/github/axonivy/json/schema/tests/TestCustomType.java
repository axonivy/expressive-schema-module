package io.github.axonivy.json.schema.tests;

import static io.github.axonivy.json.schema.tests.TestImplementationTypes.nodesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.axonivy.json.schema.annotations.CustomType;
import io.github.axonivy.json.schema.annotations.Examples;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class TestCustomType {

  @Test
  void useCustomType_field() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Examplified.class);
    JsonNode provider = schema.get("properties").get("provider");
    assertThat(provider.get("$ref").asString()).contains("/Provider");
  }

  @Test
  void useCustomType_record() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(User.class);
    JsonNode provider = schema.get("properties").get("provider");
    assertThat(provider.get("$ref").asString()).contains("/Provider");
  }

  static class Examplified {
    @CustomType(TestCustomType.Provider.class)
    public Object provider;
  }

  static record User(
      int id,
      @CustomType(TestCustomType.Provider.class) Object provider) {
  }

  static class Provider {
    public String name;
    public UUID id;
  }

  @Test
  void useCustomType_root() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Role.class);
    var role = schema.get("properties");
    var id = role.get("id");

    assertThat(id.get("$ref").asString())
        .as("custom types still get their own reference")
        .endsWith("/Identifier");
    var identifier = schema.get("$defs").get("Identifier");
    assertThat(identifier.get("type").asString())
        .as("declares a custom-type, unrelated to the real object")
        .isEqualTo("integer");
  }

  static class Role {
    Identifier id;
    String name;
  }

  @CustomType(Long.class)
  static record Identifier(Long id) {
  }

  @Test
  void customTypeInherited_type() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Blocks.class);
    System.out.println(schema.toPrettyString());

    var textId = schema.get("$defs").get(TextId.class.getSimpleName());
    assertThat(textId.get("type").asString())
        .isEqualTo("string");
  }

  static class Blocks {
    TextId id;
  }

  @MyCustom
  static record TextId(String id) {
  }

  @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @CustomType(value = String.class)
  @interface MyCustom {

  }

  @Test
  void customTypeInherited_field() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(BlockFields.class);
    System.out.println(schema.toPrettyString());

    var id = schema.get("properties").get("id");
    assertThat(id.get("type").asString())
        .isEqualTo("string");
  }

  static class BlockFields {
    @MyCustom
    Long id;
  }

  @Test
  void customTypeExamples() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyElement.class);
    var id = schema.get("properties").get("id");
    assertThat(id.propertyNames()).containsOnly("$ref", "additionalProperties");

    var rich = schema.get("$defs").get(MyRichIdentifier.class.getSimpleName());
    var examples = rich.get("examples");

    assertThat(nodesOf(examples))
        .extracting(JsonNode::asString)
        .containsExactly("f1", "f2");
  }

  static class MyElement {
    @CustomType(MyRichIdentifier.class)
    String id;
  }

  @CustomType(String.class)
  @Examples({ "f1", "f2" })
  static class MyRichIdentifier {

  }

  @Test
  void customTypeExamples_field() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyElementLocal.class);
    var id = schema.get("properties").get("id");
    assertThat(id.propertyNames()).containsOnly("$ref", "examples", "additionalProperties");

    var propertyExamples = id.get("examples");
    assertThat(nodesOf(propertyExamples))
        .extracting(JsonNode::asString)
        .containsExactly("foreverOneHundred-123");

    var richt = schema.get("$defs").get(MyRichIdentifier.class.getSimpleName());
    var examples = richt.get("examples");
    assertThat(nodesOf(examples))
        .extracting(JsonNode::asString)
        .containsExactly("f1", "f2");
  }

  static class MyElementLocal {
    @Examples({ "foreverOneHundred-123" })
    @CustomType(MyRichIdentifier.class)
    String id;
  }

}
