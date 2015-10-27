package io.probedock.junitee.dummy;

import io.probedock.junitee.generator.IDataGenerator;

/**
 * Generator to check that evaluate and after are not called.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class BeforeCrashGenerator implements IDataGenerator {

	public int count = 0;
	
	@Override
	public void generate() {
		count++;
		throw new RuntimeException("Exception to validate that the after is not called.");
	}
	
	@Override
	public void cleanup() {
		
	}
}
