package io.probedock.junitee.generator;

/**
 * Define what can be done by a data generator
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public interface IDataGenerator {
	/**
	 * Generate data before the test starts.
	 */
	void generate();
	
	/**
	 * Cleanup data after the test ends.
	 */
	void cleanup();
}
