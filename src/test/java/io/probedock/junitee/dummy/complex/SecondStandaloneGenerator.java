package io.probedock.junitee.dummy.complex;

import io.probedock.junitee.generator.CompositeDataGenerator;

/**
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class SecondStandaloneGenerator extends CompositeDataGenerator {
	
	@Override
	protected void generateData() {
		Object generatedData = getDataGenerator(FirstStandaloneGenerator.class).getGeneratedData();
	}

	@Override
	protected void cleanData() {
	}
}
