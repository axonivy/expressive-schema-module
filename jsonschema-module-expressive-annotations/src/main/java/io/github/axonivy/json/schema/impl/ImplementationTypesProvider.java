package io.github.axonivy.json.schema.impl;

import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_ANYOF;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_CONST;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_DESCRIPTION;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_ENUM;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE;
import static com.github.victools.jsonschema.generator.SchemaKeyword.TAG_TYPE_STRING;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaKeyword;

import io.github.axonivy.json.schema.annotations.Implementations;
import io.github.axonivy.json.schema.annotations.Implementations.TypeReqistry;
import io.github.axonivy.json.schema.impl.ConditionalFieldProvider.ConditionBuilder;


/**
 * Specify valid implementing types of a parent interface.
 */
public class ImplementationTypesProvider implements CustomDefinitionProviderV2 {

  private static Comparator<Class<?>> SIMPLE_NAME_SORTED = (c1,c2) -> c1.getSimpleName().compareToIgnoreCase(c2.getSimpleName());
  private final boolean conditional;
  private final Map<Class<?>, List<String>> common = new HashMap<>();

  private final Set<ResolvedType> supplying = new HashSet<>();

  public ImplementationTypesProvider(boolean conditional) {
    this.conditional = conditional;
  }

  @Override
  public CustomDefinition provideCustomSchemaDefinition(ResolvedType type, SchemaGenerationContext context) {
    var impls = type.getErasedType().getDeclaredAnnotation(Implementations.class);
    if (impls != null) {
      if (supplying.contains(type)) {
        return null;
      }
      supplying.add(type);
      return supplyVirtualContainer(type, context, impls);
    }
    var base = Optional.ofNullable(common.get(type.getErasedType()));
    if (base.isPresent()) {
      return implementation(type, context, base.get());
    }
    return null;
  }

  private CustomDefinition implementation(ResolvedType type, SchemaGenerationContext context, List<String> baseCommon) {
    var std = context.createStandardDefinition(type, this);
    if (std.get(context.getKeyword(SchemaKeyword.TAG_PROPERTIES)) instanceof ObjectNode props) {
      props.remove(baseCommon);
    }
    return new CustomDefinition(std);
  }

  private CustomDefinition supplyVirtualContainer(ResolvedType type, SchemaGenerationContext context, Implementations impls) {
    TypeReqistry registry = create(impls.value());
    var base = Optional.ofNullable(registry.base())
      .map(context.getTypeContext()::resolve)
      .orElse(type);
    ObjectNode std = context.createStandardDefinition(base, this);

    Set<Class<?>> subTypes = registry.types();
    var props = propertiesOf(context, std);
    storeCommonProps(subTypes, props);
    props.set(impls.type(), craftTypeConsts(context, registry));

    String containerName = impls.container();
    if (!containerName.isBlank()) {
      var container = props.putObject(containerName);
      if (conditional) {
        container.put("type", "object"); //vs-code needs a generic block for validation
      } else {
        container.setAll(craftAnyOf(context, subTypes));
      }
    }

    if (!conditional) {
      return new CustomDefinition(std, false);
    }
    return conditional(context, impls, registry)
      .toDefinition(()->std)
      .map(node -> new CustomDefinition(node, false))
      .orElse(null);
  }

  private void storeCommonProps(Set<Class<?>> subTypes, ObjectNode props) {
    if (!props.isEmpty()) {
      List<String> names = new ArrayList<String>();
      props.fieldNames().forEachRemaining(names::add);
      subTypes.forEach(sub -> common.put(sub, names));
    }
  }

  private static ObjectNode propertiesOf(SchemaGenerationContext context, ObjectNode std) {
    var version = context.getGeneratorConfig().getSchemaVersion();
    String propertiesTag = SchemaKeyword.TAG_PROPERTIES.forVersion(version);
    if (std.get(propertiesTag) instanceof ObjectNode props) {
      return props;
    }
    return std.putObject(propertiesTag);
  }

  private ConditionBuilder conditional(SchemaGenerationContext context, Implementations impls, TypeReqistry registry) {
    var builder = new ConditionBuilder(context);
    registry.types().stream()
      .sorted(SIMPLE_NAME_SORTED)
      .forEachOrdered(subType ->  {
        var resolved = context.getTypeContext().resolve(subType);
        ObjectNode reference = context.createStandardDefinitionReference(resolved, null);
        builder.addCondition(impls.type(), registry.typeName(subType), impls.container(), reference);
    });
    return builder;
  }

  private static TypeReqistry create(Class<? extends TypeReqistry> registryType) {
    try {
      Constructor<?> constructor = registryType.getConstructors()[0];
      return (TypeReqistry) constructor.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException("Failed to create type registry: "+registryType+". Does it have a public zero-arg constructor?", ex);
    }
  }

  private JsonNode craftTypeConsts(SchemaGenerationContext context, TypeReqistry registry) {
    var described = registry.types().stream()
      .collect(Collectors.toMap(
        type -> registry.typeName(type),
        type -> Optional.ofNullable(registry.typeDesc(type)).orElse(""))
      );
    if (described.values().stream()
        .filter(Predicate.not(String::isBlank))
        .findAny().isEmpty()) {
      return craftEnum(context, described.keySet());
    }
    return enumerateValidIds(context, described);
  }

  public static ObjectNode craftEnum(SchemaGenerationContext context, Collection<String> consts) {
    ObjectNode typeDef = context.getGeneratorConfig().createObjectNode();
    var version = context.getGeneratorConfig().getSchemaVersion();
    typeDef.put(TAG_TYPE.forVersion(version), TAG_TYPE_STRING.forVersion(version));
    ArrayNode items = typeDef.putArray(TAG_ENUM.forVersion(version));
    consts.stream()
      .sorted()
      .forEach(items::add);
    return typeDef;
  }

  public static ObjectNode enumerateValidIds(SchemaGenerationContext context, Map<String, String> described) {
    var version = context.getGeneratorConfig().getSchemaVersion();
    var consts = context.getGeneratorConfig().createObjectNode();
    consts.put(TAG_TYPE.forVersion(version), TAG_TYPE_STRING.forVersion(version));
    var anyOf = consts.putArray(TAG_ANYOF.forVersion(version));
    described.entrySet().stream().forEach(et -> {
      ObjectNode impl = anyOf.addObject();
      impl.put(TAG_CONST.forVersion(version), et.getKey());
      if (!et.getValue().isBlank()) {
        impl.put(TAG_DESCRIPTION.forVersion(version), et.getValue());
      }
    });
    return consts;
  }

  public static ObjectNode craftAnyOf(SchemaGenerationContext context, Set<Class<?>> subTypes) {
    ObjectNode typeDef = context.getGeneratorConfig().createObjectNode();
    var version = context.getGeneratorConfig().getSchemaVersion();
    var anyOf = typeDef.putArray(TAG_ANYOF.forVersion(version));
    subTypes.stream()
      .sorted(SIMPLE_NAME_SORTED)
      .map(nodeType -> context.createDefinitionReference(context.getTypeContext().resolve(nodeType)))
      .forEachOrdered(anyOf::add);
    return typeDef;
  }

}
