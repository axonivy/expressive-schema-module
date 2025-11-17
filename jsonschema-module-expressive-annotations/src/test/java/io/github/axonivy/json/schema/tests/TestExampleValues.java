package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.Examples;

class TestExampleValues {

  @Test
  void enrichedWithExamples() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Examplified.class);
    assertThat(schema.toPrettyString()).contains("superFastProvider");
  }

  static class Examplified {
    @Examples({"superFastProvider", "speedBuster", "cheapAndSlow"})
    public String provider;
  }

  @Test
  void typesWithExamples() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Container.class);
    var typeRef = schema.get("$defs").get(TypeReference.class.getSimpleName());
    var examples = (ArrayNode) typeRef.get("examples");
    assertThat(stringsOf(examples))
        .containsExactly("com.acme.MyId", "com.acme.MyId:methodRef");
  }

  static List<String> stringsOf(ArrayNode node) {
    List<String> strings = new ArrayList<>();
    node.elements().forEachRemaining(it -> strings.add(it.asText()));
    return strings;
  }

  static class Container {
    TypeReference ref;
  }

  @Examples({"com.acme.MyId", "com.acme.MyId:methodRef"})
  static class TypeReference {}

}
