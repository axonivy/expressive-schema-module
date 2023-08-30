package io.github.rew.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for a type, where not all valid properties/fields are known.
 * So any unknown property is accepted.
 *
 * @see "https://json-schema.org/understanding-json-schema/reference/object.html#additional-properties"
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdditionalProperties {
  public boolean value() default true;
}
