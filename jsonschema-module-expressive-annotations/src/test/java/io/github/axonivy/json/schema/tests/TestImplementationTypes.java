package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.ExpressiveSchemaModule.ExpressiveSchemaOption;
import io.github.axonivy.json.schema.annotations.Implementations;
import io.github.axonivy.json.schema.annotations.Implementations.TypeReqistry;

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
  void subTypes_baseProps() {
    var genericProps = defs.get(Generic.class.getSimpleName()).get("properties");
    assertThat(namesOf(genericProps))
      .as("enriched with properties from a 'base' type")
      .contains("common");

    var specificProps = defs.get(Specific.class.getSimpleName()).get("properties");
    assertThat(namesOf(specificProps))
      .as("does not restate common 'base' properties")
      .doesNotContain("common")
      .contains("customName");
  }

  @Test
  void subTypes_enumerated() {
    var props = defs.get(Generic.class.getSimpleName()).get("properties");
    var types = props.get("type").get("enum");
    assertThat(types).isInstanceOf(ArrayNode.class);

    assertThat(nodesOf(types))
      .extracting(JsonNode::asText)
      .containsExactly(
        Another.class.getSimpleName(),
        Container.class.getSimpleName(),
        Specific.class.getSimpleName()
      );
  }

  @Test
  void subTypes_allOfCondition() {
    var generic = defs.get(Generic.class.getSimpleName());
    var allOf = nodesOf(generic.get("allOf"));
    assertThat(allOf)
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
    assertThat(allOf).extracting(n -> n.get("if").get("properties").get("type").get("const").asText())
      .as("alphabetical order for constant generator results")
      .containsExactly("Another", "Container", "Specific");
  }

  @Test
  void subTypes_anyOf_unconditional() {
    var unconditional = new ExpressiveSchemaGenerator(EnumSet.noneOf(ExpressiveSchemaOption.class)).generateSchema(MyRootType.class);
    System.out.println(unconditional.toPrettyString());

    var generic = unconditional.get("$defs").get(Generic.class.getSimpleName());
    var subTypes = generic.get("properties").get("config");
    assertThat(namesOf(subTypes))
      .as("generic anyOf refs are better supported on some schema consumers: e.g json-schema-to-typescript generator")
      .containsOnly("anyOf");
    var anyOf = subTypes.get("anyOf");
    assertThat(nodesOf(anyOf))
      .extracting(n -> n.get("$ref").asText())
      .containsExactly(
        "#/$defs/Another",
        "#/$defs/Container",
        "#/$defs/Specific"
      );
  }

  static class MyRootTypeNoContainer {
    public GenericNoContainer provider;
  }

  @Implementations(value = LocalFactory.class, container = "")
  public static interface GenericNoContainer {
    String idX();
  }

  @Test
  void types_withoutContainer() {
    var unconditional = new ExpressiveSchemaGenerator().generateSchema(MyRootTypeNoContainer.class);
    System.out.println(unconditional.toPrettyString());

    var generic = unconditional.get("$defs").get(GenericNoContainer.class.getSimpleName());
    var props = generic.get("properties");
    assertThat(namesOf(props))
      .as("empty 'container' name omits its declaration: this allows custom property rules to reflect children")
      .doesNotContain("config", "");
  }


  static List<String> namesOf(JsonNode defs) {
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

  @Implementations(LocalFactory.class)
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

  public static class LocalFactory implements TypeReqistry {
    @Override
    public Set<Class<?>> types() {
      return Set.of(Specific.class, Another.class, Container.class);
    }

    @Override
    public Class<?> base() {
      return Base.class;
    }
  }

}
