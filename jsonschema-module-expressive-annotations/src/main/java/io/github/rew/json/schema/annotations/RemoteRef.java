package io.github.rew.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Expresses a '$ref' URI that refers a remote schema, such as a sibling-schema
 * that is reachable on the same host.
 *
 * @see "https://json-schema.org/understanding-json-schema/reference/combining.html"
 *
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteRef {

  /**
   * @return the value of a JSON schema <code>$ref</code> field.
   */
  public String value() default "";

  /**
   * @return refers a remote type on a Map<String, Object> for the value.
   */
  public String mapValueType() default "";
}
