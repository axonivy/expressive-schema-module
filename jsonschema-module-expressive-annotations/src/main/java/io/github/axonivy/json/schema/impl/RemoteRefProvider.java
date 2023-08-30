package io.github.axonivy.json.schema.impl;

import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_ADDITIONAL_PROPERTIES;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_REF;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE_OBJECT;

import java.util.Map;

import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;

import io.github.axonivy.json.schema.annotations.RemoteRef;

public class RemoteRefProvider implements CustomPropertyDefinitionProvider<FieldScope> {

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    RemoteRef reference = scope.getAnnotation(RemoteRef.class);
    if (reference == null) {
      return null;
    }

    var version = context.getGeneratorConfig().getSchemaVersion();
    String $ref = TAG_REF.forVersion(version);

    if (scope.getType().isInstanceOf(Map.class) && !reference.mapValueType().isBlank()) {
      var map = context.getGeneratorConfig().createObjectNode();
      map.put(TAG_TYPE.forVersion(version), TAG_TYPE_OBJECT.forVersion(version));
      var additional = map.putObject(TAG_ADDITIONAL_PROPERTIES.forVersion(version));
      additional.put($ref, DynamicRefs.resolve(reference.mapValueType()));
      return new CustomPropertyDefinition(map);
    }

    var remotRef = context.getGeneratorConfig().createObjectNode();
    remotRef.put($ref, DynamicRefs.resolve(reference.value()));
    return new CustomPropertyDefinition(remotRef);
  }

}
