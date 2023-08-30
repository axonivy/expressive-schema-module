package io.github.rew.json.schema;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

import io.github.rew.json.schema.impl.ConditionalFieldProvider;
import io.github.rew.json.schema.impl.ConfigNamingStrategy;
import io.github.rew.json.schema.impl.ExamplesProvider;
import io.github.rew.json.schema.impl.RemoteRefProvider;


public class ExpressiveSchemaModule implements Module {

  private final Set<ExpressiveSchemaOption> options;

  public ExpressiveSchemaModule(ExpressiveSchemaOption... options) {
    this(Arrays.stream(options).collect(Collectors.toSet()));
  }

  public ExpressiveSchemaModule(Set<ExpressiveSchemaOption> options) {
    this.options = options;
  }

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder configBuilder) {
    configBuilder.forTypesInGeneral()
      .withCustomDefinitionProvider(new ConditionalFieldProvider());
    configBuilder.forFields()
      .withCustomDefinitionProvider(new RemoteRefProvider())
      .withCustomDefinitionProvider(new ExamplesProvider());

    if (options.contains(ExpressiveSchemaOption.USE_ADDITIONAL_PROPERTIES_ANNOTATION)) {
      configBuilder.forTypesInGeneral()
        .withAdditionalPropertiesResolver(new io.github.rew.json.schema.impl.AdditionalPropsSupplier());
    }
    if (options.contains(ExpressiveSchemaOption.USE_JACKSON_JSON_PROPERTY_DEFAULT_VALUE)) {
      configBuilder.forFields()
        .withDefaultResolver(ExpressiveSchemaModule::jacksonDefaultVal);
    }
    if (options.contains(ExpressiveSchemaOption.USE_SCHEMA_SUFFIX_NAMING_STRATEGY)) {
      configBuilder.forTypesInGeneral()
        .withDefinitionNamingStrategy(new ConfigNamingStrategy());
    }
  }

  private static Object jacksonDefaultVal(FieldScope field) {
    JsonProperty annotation = field.getAnnotationConsideringFieldAndGetter(JsonProperty.class);
    return Optional.ofNullable(annotation)
      .map(JsonProperty::defaultValue)
      .filter(Predicate.not(String::isEmpty))
      .orElse(null);
  }

  public static enum ExpressiveSchemaOption {
    USE_ADDITIONAL_PROPERTIES_ANNOTATION,
    USE_JACKSON_JSON_PROPERTY_DEFAULT_VALUE,
    USE_SCHEMA_SUFFIX_NAMING_STRATEGY
  }
}
