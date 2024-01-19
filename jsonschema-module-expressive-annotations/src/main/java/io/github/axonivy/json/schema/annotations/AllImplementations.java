package io.github.axonivy.json.schema.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;


/**
 * Specifies all valid sub-types of a generic interface; using a 'type' property to identify the concrete sub-type at validation time.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllImplementations {

  /** the property that will define the implementing type of this generic object */
  public String type() default "type";

  /** the property that will hold the implementation specific properties */
  public String container() default "config";

  /** the provider of at least all valid-subtypes */
  public Class<? extends TypeReqistry> value();

  public static interface TypeReqistry {
    Set<Class<?>> types();
    default Class<?> base(){ return null; }
  }

}