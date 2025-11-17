package io.github.axonivy.json.schema.tests;

import static io.github.axonivy.json.schema.tests.TestImplementationTypes.namesOf;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.StringWrapper;

class TestStringWrapper {

  @Test
  void stringRecords() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Container.class);
    System.out.println(schema.toPrettyString());
    var id = schema.get("$defs").get(Id.class.getSimpleName());
    assertThat(id.get("type").asText())
        .isEqualTo("string");
    assertThat(namesOf(id))
        .as("do not declare any child properties")
        .containsOnly("type");
  }

  class Container {
    Id id;
  }

  @StringWrapper
  record Id(String theId) {}

}
