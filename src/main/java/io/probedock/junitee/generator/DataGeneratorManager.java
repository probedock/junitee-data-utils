package io.probedock.junitee.generator;

import io.probedock.junitee.dependency.DependencyInjector;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The data generator manager keep track of factories to ensure only one data generator type 
 * is instantiated during a class test execution.
 * 
 * This data generator manager should be used associated with a JUnit Rule mechanism.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DataGeneratorManager implements TestRule {
	private static final Logger LOG = LoggerFactory.getLogger(DataGeneratorManager.class);
	
	/**
	 * Entity manager factory to generate new entity manager to share between generators for a same test
	 */
	private EntityManagerFactory entityManagerFactory;
	
	/**
	 * Keep track of factories to be able to retrieve a data generator directly in a test
	 */
	private Map<Class, IDataGenerator> dataGenerators = new HashMap<>();
	
	/**
	 * Determine if a test is running or not. This is required to enable/disable
	 * the behavior of method interceptions during the test method run.
	 */
	private static Boolean testRunning = false;
	
	/**
	 * Force the construction of the data generator with an entity manager
	 * 
	 * @param entityManagerFactory Entity manager factory to create new entity managers
	 */
	public DataGeneratorManager(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				// Create an entity manager to share between the before and after phase
				EntityManager entityManager = entityManagerFactory.createEntityManager();
				
				try {
					generate(description, entityManager);
					testRunning = true;
					base.evaluate();
				}
				finally {
					testRunning = false;
					cleanup(description, entityManager);
				}
			}
		};
	}	
	
	/**
	 * Be able to retrieve a data generator
	 * 
	 * @param <T> Data generator type
	 * @param dataGeneratorClass The data generator class to lookup
	 * @return The data generator found, null otherwise
	 */
	@SuppressWarnings("unchecked")
	public <T extends IDataGenerator> T getDataGenerator(Class<T> dataGeneratorClass) {
		// Check if the data generator exists
		if (dataGenerators.containsKey(dataGeneratorClass)) {
			return (T) dataGenerators.get(dataGeneratorClass);
		}
		// Unknown data generator
		else {
			throw new RuntimeException(new DataGeneratorException("The data generator " + dataGeneratorClass.getCanonicalName() + " is not present in the annotation."));
		}
	}
	
	/**
	 * Actions to generate data
	 * 
	 * @param description The description to get test data
	 * @param entityManager The entity manager
	 * @throws Throwable Any errors 
	 */
	private void generate(Description description, EntityManager entityManager) throws DataGeneratorException {
		// Clear the generators used in a previous test. Clear must be there because 
		// there is no warranty to reach the after if a test fails.
		dataGenerators.clear();

		DataGenerator dgAnnotation = description.getAnnotation(DataGenerator.class);
		
		if (dgAnnotation == null) {
			return;
		}
		
		// Retrieve all the data generators defined for the test method.
		for (Class<? extends IDataGenerator> dataGeneratorClass : dgAnnotation.value()) {
			// Check if the data generator is already instantiated.
			if (!dataGenerators.containsKey(dataGeneratorClass)) {
				try {
					// Instantiate a new data generator proxy, inject the EJB and keep track of it.
					IDataGenerator dataGenerator = (IDataGenerator) Enhancer.create(
						dataGeneratorClass, 
						new Class[] {IDataGenerator.class}, 
						new GeneratorCallback(entityManager)
					);
					
					DependencyInjector.inject(dataGenerator, entityManager, true);
					dataGenerators.put(dataGeneratorClass, dataGenerator);
				}
				catch (Exception ex) {
					LOG.error("Injection failed during the creation of the data generator: " + dataGeneratorClass.getCanonicalName(), ex);
					throw new DataGeneratorException("Unable to instantiate the data generator " + dataGeneratorClass.getCanonicalName(), ex);
				}
			}
			else {
				LOG.error("The data generator [" + dataGeneratorClass.getCanonicalName() + "] is already instantiated. One instance of each data generator is allowed.");
				throw new DataGeneratorException("The data generator " + dataGeneratorClass.getCanonicalName() + " is already registered. "
					+ "Only one instance of each generator can be specified in the annotation.");
			}
		}
		
		try {
			entityManager.getTransaction().begin();
			Class<? extends IDataGenerator>[] dataGeneratorClass = dgAnnotation.value();
			for (int i = 0; i < dataGeneratorClass.length; i++) {
				getDataGenerator(dataGeneratorClass[i]).generate();
			}
			entityManager.getTransaction().commit();
		}
		catch (Exception e) {
			LOG.error("Unkown error", e);
			entityManager.getTransaction().rollback();
			throw new DataGeneratorException("An unexpected error occured during the data generation.", e);
		}
		finally {
			entityManager.clear();
		}
	}

	/**
	 * Actions to clean the data
	 * 
	 * @param description The description to get test data
	 * @param entityManager The entity manager
	 * @throws Throwable Any errors 
	 */
	private void cleanup(Description description, EntityManager entityManager) throws DataGeneratorException {
		DataGenerator dgAnnotation = description.getAnnotation(DataGenerator.class);
		
		if (dgAnnotation != null && dgAnnotation.executeCleanup()) {
			try {
				entityManager.getTransaction().begin();
				
				Class<? extends IDataGenerator>[] dataGeneratorClass = dgAnnotation.value();
				for (int i = dataGeneratorClass.length - 1; i >= 0; i--) {
					getDataGenerator(dataGeneratorClass[i]).cleanup();
				}
				entityManager.getTransaction().commit();
			}
			catch (Exception e) {
				LOG.error("Unknow error", e);
				entityManager.getTransaction().rollback();
				throw new DataGeneratorException("An unexpected error occured during cleanup phase.", e);
			}
			finally {
				entityManager.clear();
			}
		}
	}
	
	/**
	 * Callback class to allow lazy instantiation of annotated fields
	 */
	private static class GeneratorCallback implements MethodInterceptor {
		/**
		 * Entity manager to manage the transactions
		 */
		private EntityManager entityManager;
		
		/**
		 * Constructor
		 * 
		 * @param entityManager Entity manager
		 * @param proxiedGenerator The generator to proxy
		 */
		public GeneratorCallback(EntityManager entityManager) {
			this.entityManager = entityManager;
		}
		
		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			// Invoke create/update/delete methods encapsulated into a transaction
			if (testRunning && method.getName().startsWith("create") || method.getName().startsWith("update") || method.getName().startsWith("delete")) {
				try {
					entityManager.getTransaction().begin();
					Object result = proxy.invokeSuper(obj, args);
					entityManager.getTransaction().commit();
					return result;
				}
				catch (Throwable t) {
					if (entityManager.getTransaction().isActive()) {
						entityManager.getTransaction().rollback();
					}
					throw t;
				}
			}

			// Invoke any other method directly
			else {
				return proxy.invokeSuper(obj, args);
			}
		}
	}
}
