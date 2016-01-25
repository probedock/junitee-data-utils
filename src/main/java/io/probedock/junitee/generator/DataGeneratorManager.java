package io.probedock.junitee.generator;

import io.probedock.junitee.dependency.DependencyInjector;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

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
 * @author Laurent Prevost <laurent.prevost@lotaris.com>
 */
public class DataGeneratorManager implements TestRule {
	private static final Logger LOG = LoggerFactory.getLogger(DataGeneratorManager.class);

	/**
	 * The configuration for the data manager
	 */
	private EntityManagerHolder entityManagerHolder;

	/**
	 * Keep track of generators to be able to retrieve a data generator directly in a test
	 */
	private Map<Class, IDataGenerator> dataGenerators = new HashMap<>();

	/**
	 * Determine if a test is running or not. This is required to enable/disable
	 * the behavior of method interceptions during the test method run.
	 */
	private static Boolean testRunning = false;

	/**
	 * Force the configuration to be present
	 *
	 * @param entityManagerHolder The data manager config
	 */
	public DataGeneratorManager(EntityManagerHolder entityManagerHolder) {
		this.entityManagerHolder = entityManagerHolder;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				// Be sure the config is ready to use
				entityManagerHolder.build();

				try {
					generate(description);
					testRunning = true;
					base.evaluate();
				}
				finally {
					testRunning = false;
					cleanup(description);
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
	 * @throws Throwable Any errors
	 */
	private void generate(Description description) throws DataGeneratorException {
		// Clear the generators used in a previous test. Clear must be there because 
		// there is no warranty to reach the after if a test fails.
		dataGenerators.clear();

		DataGenerator dgAnnotation = description.getAnnotation(DataGenerator.class);

		if (dgAnnotation == null) {
			return;
		}

		// Retrieve all the data generators defined for the test method.
		for (Class<? extends IDataGenerator> dataGeneratorClass : dgAnnotation.value()) {
			EntityManager entityManager = retrieveEntityManager(dataGeneratorClass);

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
			startTransaction();
			Class<? extends IDataGenerator>[] dataGeneratorClass = dgAnnotation.value();
			for (int i = 0; i < dataGeneratorClass.length; i++) {
				getDataGenerator(dataGeneratorClass[i]).generate();
			}
			commitTransaction();
		}
		catch (Exception e) {
			LOG.error("Unkown error", e);
			rollbackTransaction();
			throw new DataGeneratorException("An unexpected error occured during the data generation.", e);
		}
		finally {
			clearEntityManagers();
		}
	}

	/**
	 * Actions to clean the data
	 *
	 * @param description The description to get test data
	 * @throws Throwable Any errors
	 */
	private void cleanup(Description description) throws DataGeneratorException {
		DataGenerator dgAnnotation = description.getAnnotation(DataGenerator.class);

		if (dgAnnotation != null && dgAnnotation.executeCleanup()) {
			try {
				startTransaction();

				Class<? extends IDataGenerator>[] dataGeneratorClass = dgAnnotation.value();
				for (int i = dataGeneratorClass.length - 1; i >= 0; i--) {
					getDataGenerator(dataGeneratorClass[i]).cleanup();
				}
				commitTransaction();
			}
			catch (Exception e) {
				LOG.error("Unknow error", e);
				rollbackTransaction();
				throw new DataGeneratorException("An unexpected error occured during cleanup phase.", e);
			}
			finally {
				clearEntityManagers();
			}
		}
	}

	/**
	 * Retrieve the entity manager corresponding to the data manager
	 *
	 * @param dataGeneratorClass The data generator class
	 * @return The corresponding entity manager
	 */
	private EntityManager retrieveEntityManager(Class<? extends IDataGenerator> dataGeneratorClass) {
		EntityManagerName entityManagerName = dataGeneratorClass.getAnnotation(EntityManagerName.class);

		if (entityManagerName != null) {
			return entityManagerHolder.getManager(entityManagerName.value());
		}
		else {
			return entityManagerHolder.getDefaultManager();
		}
	}

	/**
	 * Start the transaction on all registered entity managers
	 */
	private void startTransaction() {
		for (EntityManager em : entityManagerHolder.getManagers()) {
			em.getTransaction().begin();
		}
	}

	/**
	 * Rollback the transaction on all registered entity managers
	 */
	private void rollbackTransaction() {
		for (EntityManager em : entityManagerHolder.getManagers()) {
			em.getTransaction().rollback();
		}
	}

	/**
	 * Commit the transaction on all registered entity managers
	 */
	private void commitTransaction() {
		for (EntityManager em : entityManagerHolder.getManagers()) {
			em.getTransaction().commit();
		}
	}

	/**
	 * Clear all the registered entity managers
	 */
	private void clearEntityManagers() {
		for (EntityManager em : entityManagerHolder.getManagers()) {
			em.clear();
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
