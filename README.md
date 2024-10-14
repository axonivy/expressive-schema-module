# expressive-schema-module

[![Maven Central](https://img.shields.io/maven-central/v/io.github.axonivy/jsonschema-module-expressive-annotations.svg?label=Maven%20Central&logo=apachemaven)](https://central.sonatype.com/artifact/io.github.axonivy/jsonschema-module-expressive-annotations/)
[![build](https://github.com/axonivy/expressive-schema-module/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy/expressive-schema-module/actions/workflows/ci.yml)

An extension module for victools [jsonschema-generator](https://github.com/victools/jsonschema-generator). 
We supply frequently required json-schema hints by introducing annotations, that can be directly placed onto your domain models. This with the idea in mind, to keep you programming as much java as possible, without having to dive into the jsonschema generator specifics.

## Annotations

### @Examples

Simply annotate any field with `@Examples`, in order to supply valid values for it:

```java
@Examples({"superFastProvider", "speedBuster", "cheapAndSlow"})
public String provider;
```

Will generate a schema as follows:

```json
"properties" : {
  "provider" : {
    "type" : "string",
    "examples" : [ "superFastProvider", "speedBuster", "cheapAndSlow" ]
  }
}
```

### @RemoteRef

Simplifies the inclusion of other schemas, therefore enabling you to craft composite schemas.

```java
@RemoteRef("/ivy/a-sibling.json")
public Object sibling;
```

Will generate a schema as follows:

```json
"sibling" : {
  "$ref" : "/ivy/a-sibling.json"
}
```

### @Condition

Enables you to inject sibling schemas based on a selected constant.

```java
@Condition(ifConst  = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
public String provider;
```

Will generate a schema as follows:

```json
"properties" : {
  "provider" : {
    "type" : "string"
  }
},
"if" : {
  "properties" : {
    "provider" : {
      "const" : "azure-idp"
    }
  }
},
"then" : {
  "properties" : {
    "config" : {
      "$ref" : "/ivy/azure-config.json"
    }
  }
}
```

Note that multiple conditions can be combined by using many instances of `@Condition` on a single field:

```java
@Condition(ifConst  = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
@Condition(ifConst  = "ms-ad", thenProperty = "config", thenRef = "/ivy/ldap-config.json")
public String provider;
```



### @Implementations

Adds implementations of a generic type into the schema. It will use a virtual type and value properties in order to provide strict schema support, despite the generic class design.

```java
@Implementations(ComponentTypes)
public Component component;


public static class ComponentTypes implements TypeReqistry {
  @Override
  public Set<Class<?>> types() {
    return Set.of(Specific.class, Another.class);
  }
}
```

Will generate a schema as follows:

```json
"Component" : {
  "type" : "object",
  "properties" : {
    "type" : {
      "type" : "string",
      "enum" : [ "Another", "Specific" ]
    },
    "config" : {
      "type" : "object"
    }
  },
  "additionalProperties" : false,
  "allOf" : [ {
    "if" : {
      "properties" : {
        "type" : {
          "const" : "Specific"
        }
      }
    },
    "then" : {
      "properties" : {
        "config" : {
          "$ref" : "#/$defs/Specific"
        }
      }
    }
  } ]
}
```

### @TypesAsFields

Adds implementations of a generic type to explicit field references. 
This is perfect if you maintain a Map with well known keys in your Java objects.

```java
public CheckTypes checks;

@TypesAsFields(Checks)
public interface CheckTypes implements Map<String, MyChecker>


public static class Checks implements FieldRegistry {
  @Override
  public Set<Class<?>> types() {
    return Set.of(Specific.class, Another.class);
  }
}
```

Will generate a schema as follows:

```json
"CheckTypes" : {
  "type" : "object",
  "additionalProperties" : {
    "$ref" : "#/$defs/MyChecker"
  },
  "properties" : {
    "Another" : {
      "$ref" : "#/$defs/Another"
    },
    "Specific" : {
      "$ref" : "#/$defs/Specific"
    }
  }
}
```


### @AdditionalProperties

With the `ExpressiveSchemaOption.USE_ADDITIONAL_PROPERTIES_ANNOTATION` the schema only allows properties,  that are actually outlined in your object models.
By adding the `@AdditionalProperties` annotation however, you can allow unspecified properties again.

```java
static class AnyFieldsSchema {
  public Product product;

  @AdditionalProperties
  public static class Product {
    public String id;
  }
}
```

Produces therefore:

```json
"$schema" : "https://json-schema.org/draft/2019-09/schema",
"$defs" : {
  "Product" : {
    "type" : "object",
    "properties" : {
      "id" : {
        "type" : "string"
      }
    }
  }
},
"type" : "object",
"properties" : {
  "product" : {
    "$ref" : "#/$defs/Product"
  }
},
"additionalProperties" : false
```

### @CustomType

Allows to patch a type definition, to represent it in another way within the schema.

```java
@CustomType(TestCustomType.Provider.class)
public Object provider;
```

Generates:

```json
"properties" : {
  "provider" : {
    "$ref" : "#/$defs/Provider"
  }
}
```

## Options

The module comes with a few opt-in schema features. See the `ExpressiveSchemaOption` enumeration.

- `USE_ADDITIONAL_PROPERTIES_ANNOTATION`: disables any properties, unless you actually allow them by using the `@AdditionalProperties` annotation.
- `USE_JACKSON_JSON_PROPERTY_DEFAULT_VALUE`: contributes default values to the jsonschema by using Jacksons `@JsonPropert(defaultValue="myDefault")` annotation for the task.
- `USE_SCHEMA_SUFFIX_NAMING_STRATEGY`: provides a naming strategy that cleans your types from 'Schema' suffixes. We use this for object models that were invented just for the sake of documenting jsonschema features. So we added 'Schema' postfixes to make them distinguable from real productive objects. In the generated schema however, these long names are removed.

### Examples

Enable all options programatically:

```java
var configBuilder = new SchemaGeneratorConfigBuilder(VERSION, OptionPreset.PLAIN_JSON);
configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
configBuilder.with(new ExpressiveSchemaModule(EnumSet.allOf(ExpressiveSchemaOption.class)));
```
