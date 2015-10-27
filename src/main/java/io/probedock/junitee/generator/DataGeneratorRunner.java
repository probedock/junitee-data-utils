package io.probedock.junitee.generator;

/**
 * The data generator runner is a data structure to help to 
 * fix the order of data generators to run into a composite 
 * data generator structure.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
class DataGeneratorRunner {
	/**
	 * The data generator to run by the runner
	 */
	private IDataGenerator dataGenerator;
	
	/**
	 * Next runner should be null when the end of the chain is reached
	 */
	private DataGeneratorRunner next;
	
	/**
	 * Previous runner should be null when start of the chain is reached
	 */
	private DataGeneratorRunner previous;

	/**
	 * Constructor
	 * 
	 * @param dataGenerator The data generator to run
	 */
	public DataGeneratorRunner(IDataGenerator dataGenerator) {
		this.dataGenerator = dataGenerator;
	}

	/**
	 * Add a new data generator to the runner
	 * 
	 * @param nextDataGenerator The next data generator to run
	 * @return The data generator runner created for the next data generator
	 */
	public DataGeneratorRunner setNext(IDataGenerator nextDataGenerator) {
		next = new DataGeneratorRunner(nextDataGenerator);
		next.previous = this;
		return next;
	}
	
	/**
	 * @return The next data generator runner
	 */
	DataGeneratorRunner getNext() {
		return next;
	}

	/**
	 * @return The previous data generator runner
	 */
	DataGeneratorRunner getPrevious() {
		return previous;
	}
	
	/**
	 * @return The data generator to be run
	 */
	IDataGenerator getDataGenerator() {
		return dataGenerator;
	}
	
	/**
	 * Run the creation of the data
	 */
	public void generate() {
		dataGenerator.generate();

		if (next != null) {
			next.generate();
		}
	}
	
	/**
	 * Run the cleanup of the data
	 */
	public void cleanup() {
		dataGenerator.cleanup();
		
		if (previous != null) {
			previous.cleanup();
		}
	}
}
