package io.github.axonivy.json.schema.annotations;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a complex type as simple 'string' without any properties.
 * This can be useful if to declare 'record' types, containing only a string.
 * Note though, that you need a custom JSON serializer, to actually support this flat serialization.
 */
@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringWrapper {}
