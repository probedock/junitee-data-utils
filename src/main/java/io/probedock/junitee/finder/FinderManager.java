package io.probedock.junitee.finder;

import io.probedock.junitee.generator.DataGeneratorManager;
import io.probedock.junitee.dependency.DependencyInjector;
import java.lang.reflect.Method;
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
 * The finder manager keep track of factories to ensure only one finder type 
 * is instantiated during a class test execution.
 * 
 * This finder manager should be used associated with a JUnit Rule mechanism.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class FinderManager implements TestRule {
	private static final Logger LOG = LoggerFactory.getLogger(DataGeneratorManager.class);
	
	/**
	 * Entity manager factory to create new entity managers
	 */
	private EntityManagerFactory entityManagerFactory;

	/**
	 * Keep track of the finders
	 */
	private Map<Class, IFinder> finders = new HashMap<>();
	
	/**
	 * Force the construction of the data generator with an entity manager
	 * 
	 * @param entityManagerFactory Entity manager factory to use
	 */
	public FinderManager(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return internalApply(base, description);
	}

	/**
	 * Method to avoid problems with anonymous class and final variables otherwise
	 * the content of this method can be put on the method apply
	 */
	public Statement internalApply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				manageFinders(description);
				base.evaluate();
			}
		};
	}	
	
	/**
	 * Be able to retrieve a finder
	 * 
	 * @param <T> Finder type
	 * @param finderClass The finder class to lookup
	 * @return The finder found, null otherwise
	 */
	@SuppressWarnings("unchecked")
	public <T extends IFinder> T getFinder(Class<T> finderClass) {
		return (T) finders.get(finderClass);
	}
	
	/**
	 * Instantiate the finders
	 * 
	 * @param description The description to get test data
	 * @param context The generator context
	 * @throws Throwable Any errors 
	 */
	private void manageFinders(Description description) throws FinderException {
		// Clear the finders used in a previous test. Clear must be there because 
		// there is no warranty to reach the after if a test fails.
		finders.clear();

		Finder finderAnnotation = description.getAnnotation(Finder.class);
				
		if (finderAnnotation == null) {
			return;
		}
		
		// Create the entity manager from the factory. One entity manager is created by test executed.
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		
		// Retrieve all the data generators defined for the test method.
		for (Class<? extends IFinder> finderClass : finderAnnotation.value()) {
			// Check if the data generator is already instantiated.
			if (!finders.containsKey(finderClass)) {
				// Instantiate a new data generator, inject the DAO and keep track of it.
				finders.put(finderClass, (IFinder) Enhancer.create(finderClass, new Class[] {IFinder.class}, new FinderCallback(entityManager)));
			}
			else {
				LOG.error("The finder [" + finderClass.getCanonicalName() + "] is already instantiated. One instance of each finder is allowed.");
				throw new FinderException("The finder " + finderClass.getCanonicalName() + " is already registered. "
					+ "Only one instance of each finder can be specified in the annotation.");
			}
		}
	}
	
	/**
	 * Callback class to allow lazy instantiation of annotated fields
	 */
	private static class FinderCallback implements MethodInterceptor {
		/**
		 * Cache the fact that a the real implementation is injected or not
		 */
		private boolean injected = false;
		
		/**
		 * Keep track of the entity manager to allow the management of the cache
		 */
		private EntityManager em;

		/**
		 * Constructor
		 * 
		 * @param em The entity manager
		 */
		public FinderCallback(EntityManager em) {
			this.em = em;
		}
		
		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			// Lazy injection
			if (!injected) {
				DependencyInjector.inject(obj, em, false);
			}
			
			// Clear the entity manager cache if a find method is called
			if (method.getName().startsWith("find")) {
				em.clear();
			}
			
			// Invoke the method
			return proxy.invokeSuper(obj, args);
		}
	}
}
