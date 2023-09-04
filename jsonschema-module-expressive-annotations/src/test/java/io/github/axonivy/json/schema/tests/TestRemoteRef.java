package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.RemoteRef;

class TestRemoteRef {

  @Test
  void remoteSibling() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MySchema.class);
    JsonNode sibling = schema.get("properties").get("sibling");
    assertThat(sibling.get("$ref").asText())
      .isEqualTo("/ivy/a-sibling.json");
  }

  static class MySchema {
    @RemoteRef("/ivy/a-sibling.json")
    public Object sibling;
  }

  @Test
  void ivyYamlSubVersioned() {
    var generator = new ExpressiveSchemaGenerator();
    generator.module.property("config.version", "0.0.7");
    ObjectNode schema = generator.generateSchema(MyIvySchema.class);
    JsonNode sibling = schema.get("properties").get("sibling");
    assertThat(sibling.get("$ref").asText())
      .startsWith("/ivy/")
      .doesNotContain("config.version")
      .endsWith("/a-sibling.json");

  }

  static class MyIvySchema {
    @RemoteRef("/ivy/${config.version}/a-sibling.json")
    public Object sibling;
  }

}
