package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.AdditionalProperties;

class TestAdditionalProperties {

  @Test
  void allowsMoreProperties() {
    ObjectNode schema = ExpressiveSchemaGenerator.generateSchema(AnyFieldsSchema.class);
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
