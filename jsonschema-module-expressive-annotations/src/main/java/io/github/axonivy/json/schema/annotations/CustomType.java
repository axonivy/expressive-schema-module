package io.github.axonivy.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type hint to be used when generating json-schema.
 * Without the need to actually change the type used in the java object model.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomType {

  /**
   * @return the type that should be used for json-schema generation
   */
  public Class<?> value();

  /**
   * The name of a property in the target type; that provides further type and annotation information.
   * When set, the selected property's schema completely replaces the annotated property's schema; other properties
   * of the target type are ignored. E.g. this is helpful to supply defaults using
   * <code>@JsonProperty(defaultValue = "...")</code> in the target type.
   * @return
   */
  public String property() default "";
}
