package io.github.axonivy.json.schema.impl;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import com.github.victools.jsonschema.generator.ConfigFunction;
import com.github.victools.jsonschema.generator.TypeScope;

import io.github.axonivy.json.schema.annotations.AdditionalProperties;

public class AdditionalPropsSupplier implements ConfigFunction<TypeScope, Type> {

  @Override
  public Type apply(TypeScope scope) {
    if (scope.getType().isInstanceOf(Map.class)) {
      return scope.getTypeParameterFor(Map.class, 1); // declared value-type
    }
    if (Objects.equals(Object.class, scope.getType().getErasedType())) {
      return null; // additional properties allowed
    }
    var addProps = scope.getContext().getTypeAnnotationConsideringHierarchy(scope.getType(), AdditionalProperties.class);
    if (addProps != null && addProps.value()) {
      return null;
    }
    return Void.class; // no additional properties
  }

}