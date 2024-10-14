package io.github.axonivy.json.schema.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({FIELD, METHOD, TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TypesAsFields {

  /** the provider of at least all valid-subtypes */
  public Class<? extends FieldRegistry> value();

  public static interface FieldRegistry extends MultiTypes {

    default String fieldName(Class<?> type) {
      return type.getSimpleName();
    }

    default String fieldDescription(@SuppressWarnings("unused") Class<?> type) {
      return null;
    }

    default Class<?> valueType(Class<?> type) {
      return type;
    }

  }

}
