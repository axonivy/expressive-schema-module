package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

class TestDefaultValues {

  @Test
  void defaultValue() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(MyCar.class);
    var brand = schema.get("properties").get("brand");
    assertThat(brand.get("default").asString())
        .isEqualTo("Toyota");
  }

  private static class MyCar {
    @JsonProperty(defaultValue = "Toyota")
    public String brand;
  }
  
  @Test
  void defaultValueForList() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(MyGarage.class);
    var cars = schema.get("properties").get("cars");
    assertThat(cars.get("default").isArray()).isTrue();
    assertThat(cars.get("default").get(0).asString())
        .isEqualTo("Toyota");
    assertThat(cars.get("items").has("default")).isFalse();
  }

  private static class MyGarage {
    @JsonProperty(defaultValue = "[\"Toyota\"]")
    public java.util.List<String> cars;
  }

  @Test
  void invalidJsonDefaultForList_throws() {
    assertThatThrownBy(() -> new ExpressiveSchemaGenerator().generateSchema(InvalidGarage.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be a JSON array");
  }

  private static class InvalidGarage {
    @JsonProperty(defaultValue = "[\"Toyota\"")
    public java.util.List<String> cars;
  }

}
