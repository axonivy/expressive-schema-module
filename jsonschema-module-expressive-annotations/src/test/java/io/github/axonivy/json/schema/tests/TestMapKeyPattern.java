package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.validation.constraints.Pattern;

import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.github.axonivy.json.schema.ExpressiveSchemaModule;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class TestMapKeyPattern {

  @Test
  void mapKeys_patternRestrictions() {
    var configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON);
    configBuilder.with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES);
    configBuilder.with(new ExpressiveSchemaModule());
    var config = configBuilder.build();
    ObjectNode schema = new SchemaGenerator(config).generateSchema(DatabasesSchema.class);

    JsonNode propertyNames = schema.get("properties").get("Databases").get("propertyNames");
    assertThat(propertyNames).isNotNull();
    assertThat(propertyNames.get("pattern").asString())
        .isEqualTo("^[A-Za-z0-9_-]+$");
  }

  static class DatabasesSchema {
    public Map<@Pattern(regexp = "^[A-Za-z0-9_-]+$") String, DatabaseSchema> Databases;
  }

  static class DatabaseSchema {
    URI conection;
  }
}
