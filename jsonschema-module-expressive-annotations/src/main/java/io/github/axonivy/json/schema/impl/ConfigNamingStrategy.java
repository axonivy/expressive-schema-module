package io.github.axonivy.json.schema.impl;

import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.impl.DefinitionKey;
import com.github.victools.jsonschema.generator.naming.DefaultSchemaDefinitionNamingStrategy;

public class ConfigNamingStrategy extends DefaultSchemaDefinitionNamingStrategy {

  @Override
  public String getDefinitionNameForKey(DefinitionKey key, SchemaGenerationContext context) {
    String typeName = super.getDefinitionNameForKey(key, context);
    if (typeName.endsWith(Types.SCHEMA_SUFFIX)) { // remove repetitive 'Schema' suffix
      typeName = typeName.substring(0, typeName.length()-Types.SCHEMA_SUFFIX.length());
    }
    return typeName;
  }

  interface Types {
    String SCHEMA_SUFFIX = "Schema";
  }

}
