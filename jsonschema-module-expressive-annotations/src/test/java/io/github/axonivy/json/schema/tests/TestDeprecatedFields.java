package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TestDeprecatedFields {

  @Test
  void deprecatedField() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(Version.class);
    var props = schema.get("properties");
    assertThat(props.get("id").get("deprecated").asBoolean()).isTrue();
    assertThat(props.get("identifer").has("deprecated")).isFalse();
  }

  class Version {

    UUID identifer;

    /**
     * @deprecated Use {@link #identifer} instead.
     */
    @Deprecated(forRemoval = true, since = "12.0")
    Long id;
  }

}
