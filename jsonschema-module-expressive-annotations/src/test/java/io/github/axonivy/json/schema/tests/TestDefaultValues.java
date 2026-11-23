package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.axonivy.json.schema.annotations.CustomType;
import io.github.axonivy.json.schema.impl.JacksonDefaultValueProvider;
import io.github.axonivy.json.schema.impl.StringValueLoader;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;
import tools.jackson.databind.type.TypeFactory;

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
    @JsonProperty(defaultValue = "[\"Toyota\"] ")
    public java.util.List<String> cars;
  }

  @Test
  void invalidJsonDefaultForList_fallsBackToString() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(InvalidGarage.class);
    var defaultValue = schema.get("properties").get("cars").get("default");
    assertThat(defaultValue.isString())
        .isTrue();
    assertThat(defaultValue.asString())
        .isEqualTo("[\"Toyota\"");
  }

  private static class InvalidGarage {
    @JsonProperty(defaultValue = "[\"Toyota\"")
    public java.util.List<String> cars;
  }

  @Test
  void noWarningsForObjectishStrings() {
    var logRecorder = new LogRecorder();
    var schema = logRecorder.record(
        Logger.getLogger(JacksonDefaultValueProvider.class.getName()).getName(),
        () -> new ExpressiveSchemaGenerator().generateSchema(BlackGarage.class));
    assertThat(logRecorder.warnings()).isEmpty();

    var garage = schema.get("properties");
    assertThat(garage.get("partners").get("default").toString())
        .isEqualTo("[\"green27\"]");

    System.out.println(schema.toPrettyString());
    assertThat(garage.get("goldMembers").get("default").toString())
        .isEqualTo("[42]");
  }

  @Test
  void defaultValueLoader() {
    var loader = new StringValueLoader("id",
        TypeFactory.unsafeSimpleType(GarageId.class), // re-produce; delegated custom-type to target field miss-match
        new ObjectMapper());
    var def = loader.valueOf("#{id}");
    assertThat(def)
        .as("object declarations; fallback to string")
        .isInstanceOf(StringNode.class);
    assertThat(def.asString())
        .isEqualTo("#{id}");
  }

  private static class BlackGarage {
    @JsonProperty(defaultValue = "#{id}")
    GarageId id;

    @JsonProperty(defaultValue = "[ \"green27\" ]")
    List<GarageId> partners;

    @JsonProperty(defaultValue = "[ 42 ]")
    List<CustomId> goldMembers;

    @JsonProperty(defaultValue = "[\"ClASSIC\"]")
    List<Type> types;
  }

  @CustomType(String.class)
  private static class GarageId {
  }

  @CustomType(Integer.class)
  private static class CustomId {
  }

  private enum Type {
    ClASSIC,
    MODERN
  }

  @Test
  void defaultValueForNativeTypes() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(MyApp.class);
    var properties = schema.get("properties");

    var defVal = properties.get("isActive").get("default");
    assertThat(defVal.asBoolean())
        .isTrue();
    assertThat(defVal.isBoolean())
        .as("default is provided as native boolean")
        .isTrue();

    var retries = properties.get("retries").get("default");
    assertThat(retries.isIntegralNumber()).isTrue();
    assertThat(retries.asInt()).isEqualTo(3);

    var timeout = properties.get("timeout").get("default");
    assertThat(timeout.isIntegralNumber()).isTrue();
    assertThat(timeout.asLong()).isEqualTo(4_000_000_000L);

    var rate = properties.get("rate").get("default");
    assertThat(rate.isNumber()).isTrue();
    assertThat(rate.asDouble()).isEqualTo(1.25);
  }

  private static class MyApp {
    @JsonProperty(defaultValue = "true")
    public boolean isActive;

    @JsonProperty(defaultValue = "3")
    public int retries;

    @JsonProperty(defaultValue = "4000000000")
    public long timeout;

    @JsonProperty(defaultValue = "1.25")
    public double rate;
  }

  @Test
  void defaultValueForEnum() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(MyEnumApp.class);
    var properties = schema.get("properties");

    var defaultValue = properties.get("status").get("default");
    assertThat(defaultValue.isString()).isTrue();
    assertThat(defaultValue.asString()).isEqualTo("ACTIVE");

    var legacyDefault = properties.get("legacyStatus").get("default");
    assertThat(legacyDefault.isString()).isTrue();
    assertThat(legacyDefault.asString()).isEqualTo("ACTIVE");

    var invalidDefault = properties.get("invalidStatus").get("default");
    assertThat(invalidDefault.isString()).isTrue();
    assertThat(invalidDefault.asString()).isEqualTo("MISSING");
  }

  private static class MyEnumApp {
    @JsonProperty(defaultValue = "\"ACTIVE\"")
    public Status status;

    @JsonProperty(defaultValue = "ACTIVE")
    public Status legacyStatus;

    @JsonProperty(defaultValue = "MISSING")
    public Status invalidStatus;
  }

  private enum Status {
    ACTIVE,
    INACTIVE
  }

}
