# expressive-schema-module

[![Maven Central](https://img.shields.io/maven-central/v/io.github.axonivy/jsonschema-module-expressive-annotations.svg?label=Maven%20Central&logo=apachemaven)](https://central.sonatype.com/artifact/io.github.axonivy/jsonschema-module-expressive-annotations/)
[![build](https://github.com/axonivy/expressive-schema-module/actions/workflows/ci.yml/badge.svg)](https://github.com/axonivy/expressive-schema-module/actions/workflows/ci.yml)

An extension module for victools [jsonschema-generator](https://github.com/victools/jsonschema-generator). 
We supply frequently required json-schema hints by introducing annotations, that can be directly placed onto your domain models. This with the idea in mind, to keep you programming as much java as possible, without having to dive into the jsonschema generator specifics.

## Annotations

### @Examples

Simply annotate any field with `@Examples`, in order to supply valid values for it:

```
@Examples({"superFastProvider", "speedBuster", "cheapAndSlow"})
public String provider;
```

Will generate a schema as follows:
```
"properties" : {
  "provider" : {
    "type" : "string",
    "examples" : [ "superFastProvider", "speedBuster", "cheapAndSlow" ]
  }
}
```

### @RemoteRef

Simplifies the inclusion ob other schemas, therefore enabling you to craft composite schemas.

```
@RemoteRef("/ivy/a-sibling.json")
public Object sibling;
```

Will generate a schema as follows:
```
"sibling" : {
  "$ref" : "/ivy/a-sibling.json"
}
```


### @Condition

Enables you to inject sibling schemas based on a selected constant.

```
@Condition(ifConst  = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
public String provider;
```

Will generate a schema as follows:
```
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

```
@Condition(ifConst  = "azure-idp", thenProperty = "config", thenRef = "/ivy/azure-config.json")
@Condition(ifConst  = "ms-ad", thenProperty = "config", thenRef = "/ivy/ldap-config.json")
public String provider;
```

### @AdditionalProperties

With the `ExpressiveSchemaOption.USE_ADDITIONAL_PROPERTIES_ANNOTATION` the schema only allows properties,  that are actually outlined in your object models.
By adding the `@AdditionalProperties` annotation however, you can allow unspecified properties again.

```
static class AnyFieldsSchema {
  public Product product;

  @AdditionalProperties
  public static class Product {
    public String id;
  }
}
```

Produces therefore:

```
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