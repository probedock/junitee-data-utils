package io.probedock.junitee.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow injection of data generators into
 * other data generators.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectDataGenerator {
}
