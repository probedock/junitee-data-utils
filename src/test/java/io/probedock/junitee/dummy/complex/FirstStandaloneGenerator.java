package io.probedock.junitee.dummy.complex;

import io.probedock.junitee.generator.IDataGenerator;

/**
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class FirstStandaloneGenerator implements IDataGenerator {

	@Override
	public void generate() {
	
	}

	@Override
	public void cleanup() {
	}
	
	public Object getGeneratedData() {
		return new Object();
	}
}
