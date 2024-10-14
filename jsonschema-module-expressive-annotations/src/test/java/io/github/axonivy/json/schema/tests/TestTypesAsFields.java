package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.TypesAsFields;
import io.github.axonivy.json.schema.annotations.TypesAsFields.FieldRegistry;


class TestTypesAsFields {

  @Test
  void fields() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyRootType.class);
    JsonNode defs = schema.get("$defs");

    JsonNode props = defs.get(Collection.class.getSimpleName()).get("properties");
    assertThat(namesOf(props))
      .containsExactly("Another", "Container", "Specific");

    assertThat(props.get("Another").get("$ref").asText())
      .isEqualTo("#/$defs/Another");

    JsonNode another = defs.get("Another").get("properties");
    assertThat(namesOf(another))
      .contains("common", "version");
  }

  static class MyRootType {
    public Collection provider;
  }

  @TypesAsFields(LocalFactory.class)
  public static interface Collection extends Map<String, Generic> { }

  public static class LocalFactory implements FieldRegistry {
    @Override
    public Set<Class<?>> types() {
      return Set.of(Specific.class, Another.class, Container.class);
    }
  }

  @Test
  void customFieldName() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyRootType2.class);
    JsonNode defs = schema.get("$defs");

    JsonNode props = defs.get(CollectionCustomFieldNames.class.getSimpleName()).get("properties");
    assertThat(namesOf(props))
      .containsExactly("Another", "Container", "specialName");
  }

  static class MyRootType2 {
    public CollectionCustomFieldNames provider;
  }

  @TypesAsFields(LocalFactory2.class)
  public static interface CollectionCustomFieldNames extends Map<String, Generic> { }

  public static class LocalFactory2 implements FieldRegistry {
    @Override
    public Set<Class<?>> types() {
      return Set.of(Specific.class, Another.class, Container.class);
    }

    @Override
    public String fieldName(Class<?> type) {
      if (Specific.class == type) {
        return "specialName";
      }
      return type.getSimpleName();
    }
  }

  @Test
  void customBaseType() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyRootType3.class);
    JsonNode defs = schema.get("$defs");

    JsonNode props = defs.get(CollectionOnBase.class.getSimpleName()).get("properties");
    assertThat(namesOf(props))
      .containsExactly("Another", "Container", "Specific");

    assertThat(props.get("Another").get("$ref").asText())
      .as("ref to common base")
      .isEqualTo("#/$defs/Base");
  }

  static class MyRootType3 {
    public CollectionOnBase provider;
  }

  @TypesAsFields(LocalFactory3.class)
  public static interface CollectionOnBase extends Map<String, Generic> { }

  public static class LocalFactory3 implements FieldRegistry {
    @Override
    public Set<Class<?>> types() {
      return Set.of(Specific.class, Another.class, Container.class);
    }

    @Override
    public Class<?> valueType(Class<?> type) {
      return Base.class; // resolve all fields to the same type
    }
  }

  @Test
  void customDescription() {
    ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyRootType4.class);
    JsonNode defs = schema.get("$defs");

    JsonNode props = defs.get(DescribedCollection.class.getSimpleName()).get("properties");
    assertThat(namesOf(props))
      .containsExactly("Another", "Container", "Specific");

    JsonNode another = props.get("Another");
    assertThat(namesOf(another))
      .containsOnly("$ref", "description");
    assertThat(another.get("description").asText())
      .isEqualTo("Lorem ipsum");
  }

  static class MyRootType4 {
    public DescribedCollection provider;
  }

  @TypesAsFields(LocalFactory4.class)
  public static interface DescribedCollection extends Map<String, Generic> { }

  public static class LocalFactory4 implements FieldRegistry {
    @Override
    public Set<Class<?>> types() {
      return Set.of(Specific.class, Another.class, Container.class);
    }

    @Override
    public String fieldDescription(Class<?> type) {
      if (type == Another.class) {
        return "Lorem ipsum";
      }
      return null;
    }
  }


  static List<String> namesOf(JsonNode defs) {
    var names = new ArrayList<String>();
    defs.fieldNames().forEachRemaining(names::add);
    return names;
  }

  public static interface Generic {
    String id();
  }

  public static class Base implements Generic {
    @Override
    public String id() {
      return null;
    }

    public String common;
  }

  public static class Specific extends Base implements Generic {
    public String customName;
  }

  public static class Another extends Base implements Generic {
    public int version;
  }

  public static class Container extends Base implements Generic {
    public Generic child;
  }


}
