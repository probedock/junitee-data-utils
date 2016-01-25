package io.probedock.junitee.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityManagerName {
	/**
	 * @return Entity manager name
	 */
	String value() default "";
}
