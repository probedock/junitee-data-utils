package io.probedock.junitee.dummy;

import io.probedock.junitee.generator.IDataGenerator;

/**
 * Generator to enforce the order of the generate/cleanup run
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public abstract class AbstractOrderGenerator implements IDataGenerator {
	public static String order = "";
}
