package io.github.axonivy.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents fields which only will be present, if a sibling has a certain value.
 *
 * @see "https://json-schema.org/understanding-json-schema/reference/conditionals.html"
 * @see Condition
 *
 */
@Repeatable(Conditional.List.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Conditional {

  /**
   * @return the 'name' of a sibling property to asserted
   */
  public String ifProperty();

  /**
   * @return the 'value' to assert on the field
   */
  public String[] hasConst();

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.FIELD})
  @interface List {
      Conditional[] value();
  }

}
