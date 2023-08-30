package ch.monokellabs.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents valid example values.
 * VS-Code will use them just like classic 'enum' types: and list em as completable values.
 *
 * @see "https://json-schema.org/understanding-json-schema/reference/generic.html#id2"
 *
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Examples {

  /**
   * @return the value of a JSON schema <code>examples</code> field.
   */
  public String[] value() default "";
}
