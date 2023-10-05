package io.github.axonivy.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type hint to be used when generating json-schema.
 * Without the need to actually change the type used in the java object model.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomType {

  /**
   * @return the type that should be used for json-schema generation
   */
  public Class<?> value();
}
