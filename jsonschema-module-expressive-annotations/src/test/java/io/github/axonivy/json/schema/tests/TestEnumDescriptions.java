package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

class TestEnumDescriptions {

  @Test
  void enum_description() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(MySchema.class);
    var brand = schema.get("$defs").get("Brand");
    var oneOf = brand.get("oneOf");
    assertThat(brand.get("type").asString()).isEqualTo("string");
    assertThat(oneOf.size()).isEqualTo(3);
    var ferrari = oneOf.get(0);
    assertThat(ferrari.get("const").asString())
      .isEqualTo("FERRARI");
    assertThat(ferrari.get("description").asString())
      .as("Rich description for ENUM constant")
      .isEqualTo("Ferrari brand");
    var ford = oneOf.get(1);
    assertThat(ford.get("const").asString()).isEqualTo("FORD");
    assertThat(ford.get("description")).isNull();
    var toyota = oneOf.get(2);
    assertThat(toyota.get("const").asString()).isEqualTo("TOYOTA");
    assertThat(toyota.get("description")).isNull();
  }

  static class MySchema {
    public Brand brand;
  }
  static enum Brand {
    @JsonPropertyDescription("Ferrari brand")
    FERRARI,
    FORD,
    TOYOTA
  }

  @Test
  void enum_keepStandardIfNoDescriptions() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(MySchemaWithoutDescriptions.class);
    var brand = schema.get("$defs").get("PlainBrand");
    assertThat(brand.get("enum")).isNotNull();
    assertThat(brand.get("oneOf")).isNull();
  }

  static class MySchemaWithoutDescriptions {
    public PlainBrand brand;
  }

  static enum PlainBrand {
    Tesla,
    Renault,
    Peugeot
  }

}
