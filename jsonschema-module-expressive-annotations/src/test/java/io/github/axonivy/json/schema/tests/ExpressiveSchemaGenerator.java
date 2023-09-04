package io.github.axonivy.json.schema.tests;

import java.util.EnumSet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.github.axonivy.json.schema.ExpressiveSchemaModule;
import io.github.axonivy.json.schema.ExpressiveSchemaModule.ExpressiveSchemaOption;

public class ExpressiveSchemaGenerator {

  private static final SchemaVersion VERSION = SchemaVersion.DRAFT_2019_09;
  private final SchemaGenerator generator;
  public final ExpressiveSchemaModule module;

  public ObjectNode generateSchema(Class<?> rootType) {
    return generator.generateSchema(rootType);
  }

  public ExpressiveSchemaGenerator() {
    this.module = new ExpressiveSchemaModule(EnumSet.allOf(ExpressiveSchemaOption.class));
    var config = configBuilder().with(module).build();
    this.generator = new SchemaGenerator(config);
  }

  private static SchemaGeneratorConfigBuilder configBuilder() {
    var configBuilder = new SchemaGeneratorConfigBuilder(VERSION, OptionPreset.PLAIN_JSON);
    configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    return configBuilder;
  }

}
