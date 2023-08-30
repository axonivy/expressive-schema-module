package io.github.rew.json.schema.tests;

import java.util.EnumSet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

import io.github.rew.json.schema.ExpressiveSchemaModule;
import io.github.rew.json.schema.ExpressiveSchemaModule.ExpressiveSchemaOption;

public class ExpressiveSchemaGenerator {

  private static final SchemaVersion VERSION = SchemaVersion.DRAFT_2019_09;

  public static ObjectNode generateSchema(Class<?> rootType) {
    var configBuilder = configBuilder();
    SchemaGeneratorConfig config = configBuilder.build();
    var generator = new SchemaGenerator(config);
    var schema = generator.generateSchema(rootType);
    return schema;
  }

  public static SchemaGeneratorConfigBuilder configBuilder() {
    var configBuilder = new SchemaGeneratorConfigBuilder(VERSION, OptionPreset.PLAIN_JSON);
    configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
    configBuilder.with(new ExpressiveSchemaModule(EnumSet.allOf(ExpressiveSchemaOption.class)));
    return configBuilder;
  }

}
