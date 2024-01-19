package io.github.axonivy.json.schema.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.axonivy.json.schema.annotations.Implementations;
import io.github.axonivy.json.schema.annotations.Implementations.TypeReqistry;

class TestImplementationTypesDesc {

  ObjectNode schema = new ExpressiveSchemaGenerator().generateSchema(MyRootType.class);
  JsonNode defs = schema.get("$defs");

  @Test
  void subTypes_customIdentifier() {
    System.out.println(schema.toPrettyString());
    var props = defs.get(Generic.class.getSimpleName()).get("properties");
    var types = props.get("type").get("anyOf");
    assertThat(types).isInstanceOf(ArrayNode.class);

    assertThat(nodesOf(types))
      .extracting(JsonNode::toPrettyString)
      .contains("""
        {
          "const" : "Specific",
          "description" : "verbose Specific"
        }"""
      );
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
    public String typeDesc(Class<?> type) {
      return "verbose "+type.getSimpleName();
    }
  }

}
