package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.axonivy.json.schema.annotations.RemoteRef;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class TestRemoteRef {

  @Test
  void remoteSibling() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MySchema.class);
    JsonNode sibling = schema.get("properties").get("sibling");
    assertThat(sibling.get("$ref").asString())
        .isEqualTo("/ivy/a-sibling.json");
  }

  static class MySchema {
    @RemoteRef("/ivy/a-sibling.json")
    public Object sibling;
  }

  @Test
  void dynamicRefs_sysProps() {
    System.setProperty("config.version", "0.0.1");
    try {
      ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyIvySchema.class);
      JsonNode sibling = schema.get("properties").get("sibling");
      assertThat(sibling.get("$ref").asString())
          .startsWith("/ivy/")
          .doesNotContain("config.version")
          .endsWith("/a-sibling.json");
    } finally {
      System.clearProperty("config.version");
    }
  }

  @Test
  void dynamicRefs_props() {
    var generator = new ExpressiveSchemaGenerator();
    generator.module.property("config.version", "0.0.7");
    ObjectNode schema = generator.generateSchema(MyIvySchema.class);
    JsonNode sibling = schema.get("properties").get("sibling");
    assertThat(sibling.get("$ref").asString())
        .startsWith("/ivy/")
        .doesNotContain("config.version")
        .endsWith("/a-sibling.json");
  }

  static class MyIvySchema {
    @RemoteRef("/ivy/${config.version}/a-sibling.json")
    public Object sibling;
  }

}
