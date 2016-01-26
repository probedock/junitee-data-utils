package io.probedock.junitee.dummy.complex;

import io.probedock.junitee.generator.CompositeDataGenerator;
import io.probedock.junitee.annotations.InjectDataGenerator;

/**
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class ComplexDataGenerator extends CompositeDataGenerator {

	@InjectDataGenerator
	private FirstStandaloneGenerator firstGenerator;
	
	@InjectDataGenerator
	private SecondStandaloneGenerator secondGenerator;

	@Override
	protected void setup() {
		register(firstGenerator);
		register(secondGenerator.dependsOn(firstGenerator));
	}
	
	@Override
	protected void generateData() {
	}
	
	@Override 
	protected void cleanData() {
	}
}
