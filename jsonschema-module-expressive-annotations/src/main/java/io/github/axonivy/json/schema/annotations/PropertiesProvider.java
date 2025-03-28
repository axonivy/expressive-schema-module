package io.github.axonivy.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hint to inject dynamic properties using a {@link PropertyContributor} factory.
 * Handy to describe properties that derive from dynamic sources.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertiesProvider {

  /**
   * @return the contributor of properties
   */
  public Class<? extends PropertyContributor> value();
}
