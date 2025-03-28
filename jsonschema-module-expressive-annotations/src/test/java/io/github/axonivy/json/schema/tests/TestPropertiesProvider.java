package io.github.axonivy.json.schema.tests;

import static io.github.axonivy.json.schema.tests.TestImplementationTypes.namesOf;
import static io.github.axonivy.json.schema.tests.TestImplementationTypes.nodesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.PropertiesProvider;
import io.github.axonivy.json.schema.annotations.PropertyContributor;

class TestPropertiesProvider {

  @Test
  void dynamicProperties() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(Examplified.class);
    System.out.println(schema.toPrettyString());

    var props = schema.get("$defs").get("DynamicRunner").get("properties");
    assertThat(namesOf(props))
        .containsOnly("speed", "enabled", "passengers");

    var speed = props.get("speed");
    assertThat(speed.get("type").asText())
        .isEqualTo("string");
    assertThat(speed.get("description").asText())
        .isEqualTo("how fast do you wanna go?");
    assertThat(speed.get("default").asText())
        .isEqualTo("fast");
    assertThat(nodesOf(speed.get("examples")))
        .extracting(JsonNode::asText)
        .containsOnly("slow", "fast");

    var enabled = props.get("enabled");
    assertThat(enabled.get("type").asText())
        .isEqualTo("boolean");
    assertThat(enabled.get("default").asBoolean())
        .isEqualTo(true);

    var passengers = props.get("passengers");
    assertThat(passengers.get("type").asText())
        .isEqualTo("integer");
    assertThat(passengers.get("default").asInt())
        .isEqualTo(2);

  }

  static class Examplified {
    public DynamicRunner runner;
  }

  @PropertiesProvider(io.github.axonivy.json.schema.tests.TestPropertiesProvider.HookProperties.class)
  interface DynamicRunner {}

  public static class HookProperties implements PropertyContributor {

    @Override
    public List<Prop> contribute() {
      return List.of(
          Prop.string("speed")
              .description("how fast do you wanna go?")
              .examples(List.of("slow", "fast"))
              .defaultValue("fast"),
          Prop.bool("enabled")
              .defaultValue(true),
          Prop.integer("passengers")
              .description("amount of seats")
              .defaultValue(2));
    }

  }

}
