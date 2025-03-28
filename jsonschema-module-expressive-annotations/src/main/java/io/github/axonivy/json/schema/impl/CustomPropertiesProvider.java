package io.github.axonivy.json.schema.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.PropertiesProvider;
import io.github.axonivy.json.schema.annotations.PropertyContributor;

public class CustomPropertiesProvider implements CustomDefinitionProviderV2 {

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    var provider = javaType.getErasedType().getAnnotation(PropertiesProvider.class);
    if (provider == null) {
      return null;
    }

    var contributor = create(provider.value());
    ObjectNode reference = context.createStandardDefinition(javaType, this);
    var props = contribute(context, contributor);
    var properties = context.getKeyword(SchemaKeyword.TAG_PROPERTIES);
    reference.set(properties, props);
    return new CustomDefinition(reference);
  }

  private PropertyContributor create(Class<?> contributorType) {
    try {
      if (contributorType.isAssignableFrom(PropertyContributor.class)) {
        throw new IllegalArgumentException("not a property contributor!");
      }
      return (PropertyContributor) contributorType.getDeclaredConstructor().newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to initialize contributor " + contributorType, ex);
    }
  }

  private ObjectNode contribute(SchemaGenerationContext context, PropertyContributor contribs) {
    var properties = JsonNodeFactory.instance.objectNode();
    contribs.contribute().forEach(prop -> {
      var json = properties.putObject(prop.name());
      ResolvedType resolvedType = context.getTypeContext().resolve(prop.type());
      json.setAll(context.createDefinition(resolvedType));
      Optional.ofNullable(prop.description())
          .filter(Predicate.not(String::isBlank))
          .ifPresent(desc -> json.put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), desc));
      Optional.ofNullable(prop.defaultValue())
          .map(CustomPropertiesProvider::typeIt)
          .filter(Objects::nonNull)
          .ifPresent(def -> json.set(context.getKeyword(SchemaKeyword.TAG_DEFAULT), def));
      Optional.ofNullable(prop.examples())
          .map(e -> {
            var arr = json.arrayNode();
            e.forEach(arr::add);
            return arr;
          })
          .filter(Predicate.not(ArrayNode::isEmpty))
          .ifPresent(ex -> json.set("examples", ex));
    });
    return properties;
  }

  private static JsonNode typeIt(Object object1) {
    if (object1 instanceof String text) {
      return JsonNodeFactory.instance.textNode(text);
    }
    if (object1 instanceof Integer num) {
      return JsonNodeFactory.instance.numberNode(num);
    }
    if (object1 instanceof Boolean bol) {
      return JsonNodeFactory.instance.booleanNode(bol);
    }
    return null;
  }

}
