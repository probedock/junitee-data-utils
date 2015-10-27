package io.probedock.junitee.finder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to configure finders for a test method
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Finder {
	/**
	 * @return List of data generators to use
	 */
	Class<? extends IFinder>[] value();
}
