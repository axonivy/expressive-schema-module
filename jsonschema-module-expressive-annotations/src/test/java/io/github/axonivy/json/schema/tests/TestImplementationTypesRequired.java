package io.github.axonivy.json.schema.tests;

import static io.github.axonivy.json.schema.tests.TestImplementationTypes.nodesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;

import io.github.axonivy.json.schema.ExpressiveSchemaModule;
import io.github.axonivy.json.schema.ExpressiveSchemaModule.ExpressiveSchemaOption;
import io.github.axonivy.json.schema.annotations.Implementations;
import io.github.axonivy.json.schema.annotations.Implementations.TypeReqistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

class TestImplementationTypesRequired {

  @Test
  void required_direct() {
    var base = generateJacksonAwareSchema(Base.class);
    assertThat(base.get("required"))
        .isInstanceOf(ArrayNode.class);
    var required = (ArrayNode) base.get("required");
    assertThat(required.elements()).extracting(JsonNode::asString)
        .containsOnly("common");
  }

  public static class Base implements Generic {
    @Override
    public String id() {
      return null;
    }

    @JsonProperty(required = true)
    @JsonPropertyDescription("explanation")
    public String common;
  }

  @Test
  void required_mergeBase_andTypeIfNoDefault() {
    var defs = generateJacksonAwareSchema(MyRootType.class).get("$defs");
    var generic = defs.get(Generic.class.getSimpleName());
    assertThat(generic.propertyNames())
        .contains("required");
    var required = (ArrayNode) generic.get("required");
    assertThat(required.elements()).extracting(JsonNode::asString)
        .as("require properties are merge: from 'base' + 'type' if no default is given")
        .containsOnly("common", "type");

    var another = defs.get(Another.class.getSimpleName());
    assertThat(another.propertyNames())
        .as("concrete sub-types propagate their required properties")
        .contains("required");
    var requireAnother = (ArrayNode) another.get("required");
    assertThat(nodesOf(requireAnother)).extracting(JsonNode::asString)
        .as("inherited 'common' from 'Base' should not contribute to virtual inner type requirements")
        .containsOnly("version");
  }

  static class MyRootType {
    public Generic provider;
  }

  @Implementations(LocalFactory.class)
  public interface Generic {
    String id();
  }

  @Test
  void required_mergeContainer() {
    var schema = generateJacksonAwareSchema(MyRootNeedsContainer.class);
    var defs = schema.get("$defs");
    var generic = defs.get(GenericEnforce.class.getSimpleName());
    assertThat(generic.propertyNames())
        .contains("required");
    var required = (ArrayNode) generic.get("required");
    assertThat(required.elements()).extracting(JsonNode::asString)
        .as("require properties are merge: from 'base' + 'type' if no default is given")
        .containsOnly("common", "type", "config");
  }

  static class MyRootNeedsContainer {
    public GenericEnforce provider;
  }

  @Implementations(value = LocalFactory.class, containerRequired = true)
  public interface GenericEnforce {
    String id();
  }

  @Test
  void required_mergeContainer_ifNamed() {
    var schema = generateJacksonAwareSchema(MyRootNeedsContainerButBlank.class);
    var defs = schema.get("$defs");
    var generic = defs.get(GenericBlank.class.getSimpleName());
    assertThat(generic.propertyNames())
        .contains("required");
    var required = (ArrayNode) generic.get("required");
    assertThat(required.elements()).extracting(JsonNode::asString)
        .as("container 'config' not added; since blank container name is specified")
        .containsOnly("common", "type");
  }

  static class MyRootNeedsContainerButBlank {
    public GenericBlank provider;
  }

  @Implementations(value = LocalFactory.class, containerRequired = true, container = "")
  public interface GenericBlank {
    String id();
  }

  private static JsonNode generateJacksonAwareSchema(Class<?> rootType) {
    var module = new ExpressiveSchemaModule(EnumSet.allOf(ExpressiveSchemaOption.class));
    var config = ExpressiveSchemaGenerator.configBuilder().with(module)
        .with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED))
        .build();
    var schemaGenerator = new SchemaGenerator(config);
    return schemaGenerator.generateSchema(rootType);
  }

  public static class Specific extends Base implements Generic {
    public String customName;
  }

  public static class Another extends Base implements Generic {
    @JsonProperty(required = true)
    public int version;
  }

  public static class Container extends Base implements Generic {
    public Generic child;
  }

  public static class LocalFactory implements TypeReqistry {

    @Override
    public Class<?> defaultType() {
      return null; // no default type specified!
    }

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
