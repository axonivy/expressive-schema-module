package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.axonivy.json.schema.annotations.AdditionalProperties;

import tools.jackson.databind.node.ObjectNode;

class TestAdditionalProperties {

  @Test
  void allowsMoreProperties() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(AnyFieldsSchema.class);
    assertThat(schema.toPrettyString()).contains("additionalProperties");
  }

  static class AnyFieldsSchema {
    public Product product;

    @AdditionalProperties
    public static class Product {
      public String id;
    }
  }

}
