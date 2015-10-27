package io.probedock.junitee.generator;

import java.util.HashMap;
import java.util.Map;

/**
 * Use a composite data generator if you want to beneficiate from the power
 * of automatism around the composite data generator.
 * 
 * Once your setup of data generator is done properly, you do not have to
 * manually call the data generation by yourself.
 * 
 * Basically, you have to override the setup method to put the code you need
 * to register and setup your data generators to be run in the correct order.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public abstract class CompositeDataGenerator implements IDataGenerator {
	/**
	 * Maintains the state of the data generation order when
	 * register methods are called to register data generators
	 * in certain order.
	 */
	private DataGeneratorRunner first;
	
	/**
	 * Shortcut reference to keep the current last data generator
	 * runner registered into the composite generator. It helps to
	 * add new data generator at the end in O(1) operation.
	 */
	private DataGeneratorRunner last;
	
	/**
	 * Keep a list of dependencies that can be used into
	 * a composite data generator. That references is not enough
	 * to warranty the execution of the data generator.
	 */
	private Map<String, IDataGenerator> dataGenerators = new HashMap<>();
	
	/**
	 * Common configuration to generate numerous instances of a data
	 * with/without a prefix for a name by example.
	 */
	private int numberToGenerate = 0;
	private String prefix = null;
	
	/**
	 * Define a dependency from a data generator to another one. This
	 * allows to gain access to the data generator directly into
	 * the data generator and to have the warranty that the data generator
	 * has already run. It allows having the generated data available 
	 * into one another generator.
	 * 
	 * @param dataGenerator The data generator on which [this] depends on
	 * @return This
	 */
	public CompositeDataGenerator dependsOn(IDataGenerator dataGenerator) {
		return dependsOn(dataGenerator.getClass().getCanonicalName(), dataGenerator);
	}

	/**
	 * Define a dependency from a data generator to another one. This
	 * allows to gain access to the data generator directly into
	 * the data generator and to have the warranty that the data generator
	 * has already run. It allows having the generated data available 
	 * into one another generator.
	 * 
	 * @param dataGeneratorName The name of the data generator to have more than one instance of the same generator
	 * @param dataGenerator The data generator on which [this] depends on
	 * @return This
	 */
	public CompositeDataGenerator dependsOn(String dataGeneratorName, IDataGenerator dataGenerator) {
		if (dataGenerator == this) {
			throw new IllegalArgumentException("The data generator cannot depends on itself.");
		}
		
		if (dataGeneratorName == null) {
			throw new IllegalArgumentException("The data generator name should be provided.");
		}
		
		if (dataGenerators.containsKey(dataGeneratorName)) {
			throw new IllegalArgumentException("Data generator " + dataGeneratorName + " already registered.");
		}

		dataGenerators.put(dataGeneratorName, dataGenerator);
		return this;
	}
	
	/**
	 * Configure the number of data instances to generate.
	 * 
	 * @param number The number of data to generate
	 * @return This
	 */
	public CompositeDataGenerator numberToGenerate(int number) {
		numberToGenerate = number;
		return this;
	}
	
	/**
	 * Configure a string prefix to be used in the data generation
	 * 
	 * @param prefix The prefix to use
	 * @return This
	 */
	public CompositeDataGenerator usePrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * @return The configuration value for the number to generate
	 */
	protected int getNumberToGenerate() {
		return numberToGenerate;
	}

	/**
	 * @return The configuration value for the prefix to use
	 */
	protected String getPrefix() {
		return prefix;
	}
	
	/**
	 * Retrieve the generator from a data generator class
	 * 
	 * @param <T> The type of generator
	 * @param dataGeneratorClass The data generator class
	 * @return The generator found
	 * @exception IllegalStateException In case the data generator is not found
	 */
	protected <T> T getDataGenerator(Class<? extends T> dataGeneratorClass) {
		return getDataGenerator(dataGeneratorClass.getCanonicalName(), dataGeneratorClass);
	}

	/**
	 * Retrieve the generator from a data generator class
	 * 
	 * @param <T> The type of generator
	 * @param dataDataGeneratorName The data generator name to retrieve
	 * @param dataGeneratorClass The data generator class
	 * @return The generator found
	 * @exception IllegalStateException In case the data generator is not found
	 */
	@SuppressWarnings("unchecked")
	protected <T> T getDataGenerator(String dataDataGeneratorName, Class<? extends T> dataGeneratorClass) {
		if (!dataGenerators.containsKey(dataDataGeneratorName)) {
			throw new IllegalStateException("There is no " + dataDataGeneratorName + " data generator.");
		}
		
		return (T) dataGenerators.get(dataDataGeneratorName);
	}
	
	/**
	 * Register a data generator to be run next to the previous registered
	 * data generator. The execution order will be the same as the order
	 * of the call to register method.
	 * 
	 * @param dataGenerator The data generator to register
	 */
	protected final void register(IDataGenerator dataGenerator) {
		if (first == null) {
			first = last = new DataGeneratorRunner(dataGenerator);
		}
		else {
			last = last.setNext(dataGenerator);
		}
	}
	
	/**
	 * The setup method must be override to setup the different
	 * things in the composite generator. 
	 * 
	 * In this method, you should call other methods like register 
	 * or dependsOn and so on. All the stuff to configure correctly
	 * your data generator.
	 * 
	 * The setup method is always run before the data generation
	 */
	protected void setup() { };
		
	/**
	 * Generate method should be implemented to generate data
	 * after all other data are generated.
	 * 
	 * In that method, you should have access to other generators
	 * and the result of the data generation.
	 */
	protected abstract void generateData();
	
	@Override
	public final void generate() {
		setup();
		
		if (first  != null) {
			first.generate();
		}

			generateData();
	}

	/**
	 * Generate method should be implemented to cleanup data
	 * before all other data are cleanup.
	 * 
	 * In that method, you should have access to other generators
	 * and the result of the data generation.
	 */
	protected abstract void cleanData();
	
	@Override
	public final void cleanup() {
		cleanData();
		
		if (last != null) {
			last.cleanup();
		}
	}
}
