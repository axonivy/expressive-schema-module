package io.github.axonivy.json.schema;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

import io.github.axonivy.json.schema.impl.ConditionalFieldProvider;
import io.github.axonivy.json.schema.impl.ConfigNamingStrategy;
import io.github.axonivy.json.schema.impl.CustomTypeProvider;
import io.github.axonivy.json.schema.impl.DynamicRefs;
import io.github.axonivy.json.schema.impl.ExamplesProvider;
import io.github.axonivy.json.schema.impl.ImplementationTypesProvider;
import io.github.axonivy.json.schema.impl.RemoteRefProvider;


public class ExpressiveSchemaModule implements Module {

  private final Set<ExpressiveSchemaOption> options;
  private final DynamicRefs refs = new DynamicRefs();

  public ExpressiveSchemaModule(ExpressiveSchemaOption... options) {
    this(Arrays.stream(options).collect(Collectors.toSet()));
  }

  public ExpressiveSchemaModule(Set<ExpressiveSchemaOption> options) {
    this.options = options;
  }

  public ExpressiveSchemaModule property(String key, String value) {
    refs.property(key, value);
    return this;
  }

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder configBuilder) {
    configBuilder.forTypesInGeneral()
      .withCustomDefinitionProvider(new ConditionalFieldProvider(refs));
    configBuilder.forFields()
      .withCustomDefinitionProvider(new RemoteRefProvider(refs))
      .withCustomDefinitionProvider(new ExamplesProvider())
      .withCustomDefinitionProvider(new CustomTypeProvider());

    if (options.contains(ExpressiveSchemaOption.USE_ADDITIONAL_PROPERTIES_ANNOTATION)) {
      configBuilder.forTypesInGeneral()
        .withAdditionalPropertiesResolver(new io.github.axonivy.json.schema.impl.AdditionalPropsSupplier());
    }
    if (options.contains(ExpressiveSchemaOption.USE_JACKSON_JSON_PROPERTY_DEFAULT_VALUE)) {
      configBuilder.forFields()
        .withDefaultResolver(ExpressiveSchemaModule::jacksonDefaultVal);
    }
    if (options.contains(ExpressiveSchemaOption.USE_SCHEMA_SUFFIX_NAMING_STRATEGY)) {
      configBuilder.forTypesInGeneral()
        .withDefinitionNamingStrategy(new ConfigNamingStrategy());
    }
    applyImplementations(configBuilder);
  }

  private void applyImplementations(SchemaGeneratorConfigBuilder configBuilder) {
    boolean conditionals = options.contains(ExpressiveSchemaOption.PREFER_CONDITIONAL_SUBTYPES);
    configBuilder.forTypesInGeneral()
      .withCustomDefinitionProvider(new ImplementationTypesProvider(conditionals));
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
    USE_SCHEMA_SUFFIX_NAMING_STRATEGY,
    PREFER_CONDITIONAL_SUBTYPES
  }

}
