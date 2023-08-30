package io.github.axonivy.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents conditionals for fields.
 * Therefore letting you specify properties that should only be present, if this field has an expected value.
 *
 * @see "https://json-schema.org/understanding-json-schema/reference/conditionals.html"
 *
 */
@Repeatable(Condition.List.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {

  /**
   * @return the 'value' to assert on the field, which will return the optional property if matching.
   */
  public String ifConst();

  /**
   * @return the 'name' of a property to be inserted, if the condition is met
   */
  public String thenProperty();

  /**
   * @return a '$ref' value.
   */
  public String thenRef();

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD})
  @interface List {
      Condition[] value();
  }
}
