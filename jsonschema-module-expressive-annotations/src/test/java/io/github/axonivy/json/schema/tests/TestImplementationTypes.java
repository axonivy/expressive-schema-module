package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.AllImplementations;
import io.github.axonivy.json.schema.annotations.AllImplementations.TypeReqistry;

class TestImplementationTypes {

  ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyRootType.class);
  JsonNode defs = schema.get("$defs");

  @Test
  void subTypes_partOfSchemaDefs() {
    System.out.println(schema.toPrettyString());

    assertThat(namesOf(defs))
      .as("sub-types are known and specified in the json-schema")
      .contains(
        Specific.class.getSimpleName(),
        Another.class.getSimpleName()
      );
  }

  @Test
  void subTypes_genericRefs() {
    var props = defs.get(Generic.class.getSimpleName()).get("properties");
    var propNames = namesOf(props);
    assertThat(propNames)
      .as("virtual properties injected by using the AllImplementations annotation")
      .contains("type", "config");
    assertThat(propNames)
      .as("enriched with properties from a 'base' type")
      .contains("common");

    var types = props.get("type").get("enum");
    assertThat(types).isInstanceOf(ArrayNode.class);

    var values = new ArrayList<JsonNode>();
    types.elements().forEachRemaining(values::add);
    assertThat(values)
      .extracting(JsonNode::asText)
      .contains(
        Another.class.getSimpleName(),
        Specific.class.getSimpleName()
      );
  }

  @Test
  void subTypes_enumerated() {
    var props = defs.get(Generic.class.getSimpleName()).get("properties");
    var types = props.get("type").get("enum");
    assertThat(types).isInstanceOf(ArrayNode.class);

    assertThat(nodesOf(types))
      .extracting(JsonNode::asText)
      .contains(
        Another.class.getSimpleName(),
        Specific.class.getSimpleName()
      );
  }

  @Test
  void subTypes_allOfCondition() {
    var generic = defs.get(Generic.class.getSimpleName());
    var allOf = generic.get("allOf");
    assertThat(nodesOf(allOf))
      .extracting(JsonNode::toPrettyString)
      .contains("""
        {
          "if" : {
            "properties" : {
              "type" : {
                "const" : "Specific"
              }
            }
          },
          "then" : {
            "properties" : {
              "config" : {
                "$ref" : "#/$defs/Specific"
              }
            }
          }
        }""");
  }

  private static List<String> namesOf(JsonNode defs) {
    var names = new ArrayList<String>();
    defs.fieldNames().forEachRemaining(names::add);
    return names;
  }

  private static List<JsonNode> nodesOf(JsonNode types) {
    var values = new ArrayList<JsonNode>();
    types.elements().forEachRemaining(values::add);
    return values;
  }

  static class MyRootType {
    public Generic provider;
  }

  @AllImplementations(LocalFactory.class)
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

  public static class Specific extends Base {
    public String customName;
  }

  public static class Another extends Base {
    public int version;
  }

  public static class LocalFactory implements TypeReqistry {
    @Override
    public Set<Class<?>> types() {
      return Set.of(Specific.class, Another.class);
    }

    @Override
    public Class<?> base() {
      return Base.class;
    }
  }

}