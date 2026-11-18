package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.node.ObjectNode;

class TestMapKeyPattern {

  @Test
  void mapKeys_patternRestrictions() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(DatabasesSchema.class);
    var dbs = (ObjectNode) schema.get("properties").get("Databases");
    assertThat(dbs.propertyNames())
      .contains("$ref", "propertyNames");
    assertThat(dbs.get("propertyNames")
      .get("pattern").asString())
      .isEqualTo("^[A-Za-z0-9_-]+$");
  }

  static class DatabasesSchema {
    public Map<@Pattern(regexp = "^[A-Za-z0-9_-]+$") String, DatabaseSchema> Databases;
  }

  static class DatabaseSchema {
    URI conection;
  }

  @Test
  void mapKeys_patternRestrictions_delegateType() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(DatabasesSchemaDelegate.class);
    var dbs = (ObjectNode) schema.get("properties").get("Databases");
    assertThat(dbs.propertyNames())
      .contains("$ref", "propertyNames");
    assertThat(dbs.get("propertyNames")
      .get("pattern").asString())
      .isEqualTo("^[A-Za-z0-9_-]+$");
  }

  @Test
  void mapKeys_patternRestrictions_genericIntermediateType() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(GenericMapKeyPatternSchema.class);
    var dbs = (ObjectNode) schema.get("properties").get("Databases");
    assertThat(dbs.propertyNames())
      .contains("$ref", "propertyNames");
    assertThat(dbs.get("propertyNames")
      .get("pattern").asString())
      .isEqualTo("^[A-Za-z0-9_-]+$");
  }

  @Test
  void mapKeys_patternRestrictions_reorderedGenericIntermediateType() {
    var schema = new ExpressiveSchemaGenerator().generateSchema(ReorderedMapKeyPatternSchema.class);
    var dbs = (ObjectNode) schema.get("properties").get("Databases");
    assertThat(dbs.propertyNames())
      .contains("$ref", "propertyNames");
    assertThat(dbs.get("propertyNames")
      .get("pattern").asString())
      .isEqualTo("^[A-Za-z0-9_-]+$");
  }

  static class DatabasesSchemaDelegate {
    public Databases Databases;
  }

  static class GenericMapKeyPatternSchema {
    public KeyRestrictedDatabases Databases;
  }

  static class MapBase<K, V> extends HashMap<K, V> {
  }

  static class KeyRestrictedDatabases extends MapBase<@Pattern(regexp = "^[A-Za-z0-9_-]+$") String, DatabaseSchema> {
  }

  static class ReorderedMapKeyPatternSchema {
    public ReorderedDatabases Databases;
  }

  static class ReorderedMapBase<K, V> extends HashMap<K, V> {
  }

  static class ReorderedMap<X, Y> extends ReorderedMapBase<Y, X> {
  }

  static class ReorderedDatabases extends ReorderedMap<DatabaseSchema, @Pattern(regexp = "^[A-Za-z0-9_-]+$") String> {
  }

  /** MAP delegate: Hiding Generics */
  public class Databases extends HashMap<@Pattern(regexp = "^[A-Za-z0-9_-]+$") String, DatabaseSchema> {
  }
}
