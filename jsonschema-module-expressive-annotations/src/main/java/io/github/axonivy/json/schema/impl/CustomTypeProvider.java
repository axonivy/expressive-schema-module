package io.github.axonivy.json.schema.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.victools.jsonschema.generator.AnnotationHelper;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinition.AttributeInclusion;
import com.github.victools.jsonschema.generator.CustomDefinition.DefinitionType;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.CustomPropertyDefinition;
import com.github.victools.jsonschema.generator.CustomPropertyDefinitionProvider;
import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.CustomType;

public class CustomTypeProvider implements CustomPropertyDefinitionProvider<FieldScope>, CustomDefinitionProviderV2 {

  private static final Predicate<Annotation> CONSIDER_NESTED = a -> !a.toString().startsWith("@java.lang.annotation");
  private final ThreadLocal<PropertyReplacement> activeReplacement = new ThreadLocal<>();
  private final ThreadLocal<Set<ReplacementKey>> activeProjections = new ThreadLocal<>();

  public CustomDefinitionProviderV2 propertyReplacementProvider() {
    return this::providePropertyReplacement;
  }

  @Override
  public CustomPropertyDefinition provideCustomSchemaDefinition(FieldScope scope, SchemaGenerationContext context) {
    var customType = scope.getAnnotation(CustomType.class, CONSIDER_NESTED);
    if (customType == null) {
      return null;
    }

    var resolvedType = context.getTypeContext().resolve(customType.value());
    if (!customType.property().isEmpty()) {
      var replacementScope = resolveProperty(resolvedType, customType.property(), scope, context);
      if (isActiveProjection(resolvedType, replacementScope)) {
        return null;
      }
      return resolveProperty(scope, context, resolvedType, replacementScope);
    }

    var definition = context.createDefinitionReference(resolvedType);
    return new CustomPropertyDefinition(definition);
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType javaType, SchemaGenerationContext context) {
    var resolvedTypeOpt = resolveCustomTypeFor(javaType, context);
    if (resolvedTypeOpt.isEmpty()) {
      return null;
    }
    var definition = context.createStandardDefinition(resolvedTypeOpt.get(), this);
    return new CustomDefinition(definition);
  }

  public static Optional<ResolvedType> resolveCustomTypeFor(ResolvedType javaType, SchemaGenerationContext context) {
    var reference = AnnotationHelper.resolveAnnotation(javaType.getErasedType(), CustomType.class, CONSIDER_NESTED);
    if (reference.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(context.getTypeContext().resolve(reference.get().value()));
  }

  private CustomDefinition providePropertyReplacement(ResolvedType javaType, SchemaGenerationContext context) {
    PropertyReplacement replacement = activeReplacement.get();
    if (replacement == null || !replacement.type().equals(javaType)) {
      return null;
    }

    activeReplacement.remove();
    var key = new ReplacementKey(replacement.type(), replacement.scope().getMember().getRawMember());
    var projections = activeProjections.get();
    if (projections == null) {
      projections = new HashSet<>();
      activeProjections.set(projections);
    }
    projections.add(key);
    try {
      var definition = context.createStandardDefinition(replacement.scope(), null);
      var description = replacement.scope()
          .getAnnotationConsideringFieldAndGetter(JsonPropertyDescription.class, CONSIDER_NESTED);
      if (description != null && !description.value().isBlank()) {
        definition.put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), description.value());
      }
      return new CustomDefinition(definition, DefinitionType.STANDARD, AttributeInclusion.NO);
    } finally {
      projections.remove(key);
      if (projections.isEmpty()) {
        activeProjections.remove();
      }
    }
  }

  private CustomPropertyDefinition resolveProperty(FieldScope scope, SchemaGenerationContext context,
      ResolvedType resolvedType, FieldScope replacementScope) {
    var replacement = new PropertyReplacement(resolvedType, replacementScope);
    var previous = activeReplacement.get();
    activeReplacement.set(replacement);
    try {
      return new CustomPropertyDefinition(context.createDefinitionReference(resolvedType), AttributeInclusion.NO);
    } finally {
      if (previous == null) {
        activeReplacement.remove();
      } else {
        activeReplacement.set(previous);
      }
    }
  }

  private static FieldScope resolveProperty(ResolvedType replacementType, String propertyName, FieldScope source,
      SchemaGenerationContext context) {
    ResolvedTypeWithMembers members = context.getTypeContext().resolveWithMembers(replacementType);
    return Arrays.stream(members.getMemberFields())
        .filter(field -> field.getName().equals(propertyName))
        .findFirst()
        .map(field -> context.getTypeContext().createFieldScope(field,
            new MemberScope.DeclarationDetails(field.getDeclaringType(), members)))
        .orElseThrow(() -> new IllegalArgumentException("No property '" + propertyName + "' found on replacement type '"
            + replacementType.getErasedType().getName() + "' for field '" + source.getName() + "'"));
  }

  private boolean isActiveProjection(ResolvedType type, FieldScope scope) {
    Set<ReplacementKey> projections = activeProjections.get();
    return projections != null
        && projections.contains(new ReplacementKey(type, scope.getMember().getRawMember()));
  }

  private record PropertyReplacement(ResolvedType type, FieldScope scope) {}

  private record ReplacementKey(ResolvedType type, Field field) {}

}
