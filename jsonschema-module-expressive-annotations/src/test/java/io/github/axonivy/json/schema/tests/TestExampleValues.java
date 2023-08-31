package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.Examples;

class TestExampleValues {

  @Test
  void enrichedWithExamples() {
    ObjectNode schema = ExpressiveSchemaGenerator.generateSchema(Examplified.class);
    assertThat(schema.toPrettyString()).contains("superFastProvider");
  }

  static class Examplified {
    @Examples({"superFastProvider", "speedBuster", "cheapAndSlow"})
    public String provider;
  }

}
