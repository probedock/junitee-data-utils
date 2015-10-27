package io.probedock.junitee.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to configure Data Generators for a test method.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataGenerator {
	/**
	 * @return List of data generators to use
	 */
	Class<? extends IDataGenerator>[] value();
	
	/**
	 * @return Define if after must be run
	 */
	boolean executeCleanup() default true;
}
