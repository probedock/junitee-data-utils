package io.probedock.junitee.dependency;

import io.probedock.junitee.annotations.InjectDataGenerator;
import io.probedock.junitee.annotations.UseMock;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Utility class to inject objects correctly into an object
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DependencyInjector {
	private DependencyInjector() {}

	/**
	 * Analyzes the object to get the fields annotated for injections
	 * and inject to those fields the entity manager given.
	 * <p>
	 * During the injection, new instances of DAO are created
	 * 
	 * @param obj The object to get all the fields that must be injected
	 * @param em Entity manager to inject across all the object graph
	 * @param injectGenerators Define if the data generators must be injected or not
	 */
	public static void inject(Object obj, EntityManager em, boolean injectGenerators) {
		inject(obj.getClass(), obj, em, new HashMap<String, Object>(), new HashMap<String, Object>(), new HashSet<String>(), obj.getClass().getSimpleName(), injectGenerators);
	}
	
	/**
	 * Manage the injections into an object. 
	 * 
	 * <ul>
	 *	<li>Take care to inject existing/new instances of @EJB objects into fields of objects recursively into the object graph.</li>
	 *  <li>Take care to inject existing/new instances of IDataGenerators (through @InjectDataGenerator) into fields of objects recursively into the object graph.</li>
	 *	<li>Take care to inject the provided Entity Manager (through @PersistenceContext) into the objects. Applied recursively</li>
	 * </ul>
	 * 
	 * When an EJB should be injected, the method take care to reuse an already existing instance. For the data generator, we ensure
	 * that new instances are created except in the case that the object graph created a circular dependency. In that case, an exception
	 * is raised. For the entity manager, it will be injected in @EJB and @InjectDataGenerator objects (recursively and across the class hierarchy)
	 * 
	 * @param cl The class of the object to inject (will be super class in recursive calls)
	 * @param obj The object instance of the class to inject
	 * @param em The entity managers to injects
	 * @param ejbRegistry The registry of EJB to ensure there is no two instances of the same EJB
	 * @param dgRegistry The data generator to ensure there is no two instances of data generator for a same path
	 * @param path The path where the injection occur to be able to determine if a circular dependency injection is detected
	 * @param injectGenerators Define if the data generators must be injected or not
	 */
	private static void inject(Class cl, Object obj, EntityManager em, Map<String, Object> mockRegistry, Map<String, Object> ejbRegistry, Set<String> dgRegistry, String path, boolean injectGenerators) {
		// Inject in the super class fields if any
		if (cl.getSuperclass() != Object.class) {
			inject(cl.getSuperclass(), cl.getSuperclass().cast(obj), em, mockRegistry, ejbRegistry, dgRegistry, path, injectGenerators);
		}
		
		// Get all the declared fields
		for (Field declaredField : cl.getDeclaredFields()) {
			try {
				if (DependencyInjectorHelper.isNull(declaredField, obj)) {
					Object declaredFieldObjectInstantiated = null;
					
					// Register the possible mocks to use during the chaing invocation
					if (declaredField.getAnnotation(UseMock.class) != null) {
						for (Class mockClass : declaredField.getAnnotation(UseMock.class).value()) {
							DependencyInjectorHelper.instantiateMock(mockClass, mockRegistry);
						}
					}
					
					// Manage the EJB instantiation
					if (declaredField.getAnnotation(EJB.class) != null) {
						declaredFieldObjectInstantiated = DependencyInjectorHelper.instantiateEjb(declaredField, mockRegistry, ejbRegistry);
					} 
					
					// Manage the DG instantiation
					else if (injectGenerators && declaredField.isAnnotationPresent(InjectDataGenerator.class)) {
						declaredFieldObjectInstantiated = DependencyInjectorHelper.instantiateDataGenerator(declaredField, path, dgRegistry);
					} 
					
					// Manage the EM injection
					else if (declaredField.isAnnotationPresent(PersistenceContext.class)) {
						DependencyInjectorHelper.injectField(declaredField, obj, em);
					}
					
					// Inject the field and do the injections into it
					if (declaredFieldObjectInstantiated != null) {
						DependencyInjectorHelper.injectField(declaredField, obj, declaredFieldObjectInstantiated);
						inject(declaredFieldObjectInstantiated.getClass()	, declaredFieldObjectInstantiated, em, mockRegistry, ejbRegistry, dgRegistry, path + "." + declaredField.getName(), injectGenerators);
					}
				}
			}
			catch (DependencyInjectionException die) {
				throw new RuntimeException(die);
			}
		}
	}
}
