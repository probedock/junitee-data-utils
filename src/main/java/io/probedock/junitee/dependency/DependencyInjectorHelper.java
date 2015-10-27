package io.probedock.junitee.dependency;

import io.probedock.junitee.utils.InflectorHelper;
import io.probedock.junitee.utils.NoValidClassException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;

/**
 * Dependency injector helper offers method to facilitate the injections
 * of fields into objects and such things.
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DependencyInjectorHelper {
	/**
	 * Empty constructor
	 */
	private DependencyInjectorHelper() {}
	
	/**
	 * Inject something into a field of the holder with the value provided
	 * 
	 * @param field The field to inject
	 * @param holder The object where the field is
	 * @param value The value to set to the field on the object
	 * @throws DependencyInjectionException  Any error during the injection
	 */
	static void injectField(Field field, Object holder, Object value) throws DependencyInjectionException {
		// Try to inject the entity manager to the DAO.
		boolean fieldMustBeChanged = field.isAccessible();
		if (!fieldMustBeChanged) {
			field.setAccessible(true);
		}
			
		try {
			field.set(holder, value);
		}
		catch (IllegalAccessException | IllegalArgumentException | SecurityException ex) {
			throw new DependencyInjectionException("Unable to set the value [" + value + "] on field ["
				+ field.getName() + "] of the object [" + holder.getClass().getSimpleName() + "].", ex);
		}
		finally {
			if (!fieldMustBeChanged) {
				field.setAccessible(false);
			}
		}
	}

	/**
	 * Find an implementation class from a class that is provided
	 * 
	 * @param cl The class to get the real implementation class
	 * @return The real implementation class that can be instantiated
	 * @throws DependencyInjectionException Unable to find a valid class that can be instantiated
	 */
	static Class findImplementationClass(Class cl) throws DependencyInjectionException {
		// Instantiate a new object based on class name (check if it is an interface or a concrete class).
		try {
			return InflectorHelper.retrieveInstantiableClassName(cl);
		}
		catch (NoValidClassException ex) {
			throw new  DependencyInjectionException("Unable to instantiate a new object from type [" + cl.getCanonicalName() + "]. Maybe there is no empty constructor.", ex);
		}		
	}
	
	/**
	 * Get the interface for which the mock should be used
	 * 
	 * @param mockClass The mock class for which the investigation must be done
	 * @return The interface which is annotated with @Local or @Remote
	 */
	@SuppressWarnings("unchecked")
	static Class getMockInterface(Class mockClass) {
		Class current = mockClass;
		
		do {
			// Check each interface to see if there is an interface which elegible to be mocked
			for (Class interfaceClass : current.getInterfaces()) {
				if (interfaceClass.getAnnotation(Local.class) != null || interfaceClass.getAnnotation(Remote.class) != null) {
					return interfaceClass;
				}
			}
			current = current.getSuperclass();
		} while (current != Object.class);
		
		return null;
	}
	
	/**
	 * Instantiate a new Mock instance of a class
	 * 
	 * @param mockClass The class of the mock
	 * @param mockRegistry The mock registry to get a singleton mock
	 * @return The mock instantiated or get from the cache
	 * @throws DependencyInjectionException  Any error during the mock instantiation
	 */
	static Object instantiateMock(Class mockClass, Map<String, Object> mockRegistry) throws DependencyInjectionException {
		Class mockInterfaceClass = getMockInterface(mockClass);
		
		if (mockInterfaceClass == null) {
			throw new DependencyInjectionException("There is no suitable interfaces for class: " + mockClass.getCanonicalName());
		}
		
		if (mockRegistry.containsKey(mockInterfaceClass.getCanonicalName())) {
			return mockRegistry.get(mockInterfaceClass.getCanonicalName());
		}
		else {
			try {
				Object instanceOfImplementationClass = mockClass.newInstance();
				mockRegistry.put(mockInterfaceClass.getCanonicalName(), instanceOfImplementationClass);
				return instanceOfImplementationClass;
			}
			catch (IllegalAccessException | InstantiationException e) {
				throw new DependencyInjectionException("Unable to instantiate the Mock class: " + mockClass.getCanonicalName(), e);
			}
		}
	}
	/**
	 * Instantiate a new EJB instance of a class
	 * 
	 * @param ejbField The field that is marked to be injected with an EJB class instance
	 * @param mockRegistry The register of mocks to inject in place of real implementations
	 * @param ejbRegistry The EJB registry to reuse existing instance of EJB
	 * @return The new/reused instance of the EJB ready to be injected
	 * @throws DependencyInjectionException Error during the instantiation of the new EJB
	 */
	static Object instantiateEjb(Field ejbField, Map<String, Object> mockRegistry, Map<String, Object> ejbRegistry) throws DependencyInjectionException {
		Class implementationClass = findImplementationClass(ejbField.getType());
		
		if (ejbRegistry.containsKey(implementationClass.getCanonicalName())) {
			return ejbRegistry.get(implementationClass.getCanonicalName());
		}
		else {
			try {
				// Prepare the object storage
				Object instanceOfImplementationClass;
				
				// Check if the EJB field is mockabe
				Class mockInterfaceClass = getMockInterface(implementationClass);
				if (mockInterfaceClass!= null && mockRegistry.containsKey(mockInterfaceClass.getCanonicalName())) {
					instanceOfImplementationClass = mockRegistry.get(mockInterfaceClass.getCanonicalName());
				}
				else {
					instanceOfImplementationClass = implementationClass.newInstance();
				}
				
				// Invoke the @PostConstruct methods
				invokePostConstruct(instanceOfImplementationClass);
				
				// Store in cache the EJB instance
				ejbRegistry.put(implementationClass.getCanonicalName(), instanceOfImplementationClass);
				
				return instanceOfImplementationClass;
			}
			catch (IllegalAccessException | InstantiationException e) {
				throw new DependencyInjectionException("Unable to instantiate the EJB.", e);
			}
		}
	}
	
	/**
	 * Invoke the methods which are annotated with @PostConstruct
	 * 
	 * @param obj The object to investigate
	 * @throws DependencyInjectionException Any error due to the invocation of the post construct methods
	 */
	static void invokePostConstruct(Object obj) throws DependencyInjectionException {
		Class cl = obj.getClass();
		
		// Check across the class hierarchy
		do {
			// Retrieve the methods
			for (Method m : cl.getDeclaredMethods()) {
				// Check if @PostConstruct is present
				if (m.getAnnotation(PostConstruct.class) != null) {
					try {
						boolean isAccessible = m.isAccessible();
						if (!isAccessible) {
							m.setAccessible(true);
						}
						
						m.invoke(obj);
						
						if (!isAccessible) {
							m.setAccessible(false);
						}
					}
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new DependencyInjectionException("Unable to call the method " + m.getName() + " on object of class " + 
							cl.getCanonicalName() + ". @PostConstruct methods must be empty args methods.");
					}
				}
			}

			cl = cl.getSuperclass();
		} while (cl != Object.class);
	}
	
	/**
	 * Instantiate a new Data Generator instance of a class
	 * 
	 * @param generatorField The field that is marked to be injected with a InjectDataGenerator instance
	 * @param path The current path to detect circular dependency injection
	 * @param generatorRegistry The data generator registry to check the circular dependency injection
	 * @return The new instance of the data generator ready to be injected
	 * @throws DependencyInjectionException Error during the instantiation of the new Data Generator
	 */
	static Object instantiateDataGenerator(Field generatorField, String path, Set<String> generatorRegistry) throws DependencyInjectionException {
		String currentPath = path + "." + generatorField.getName();
		Class implementationClass = findImplementationClass(generatorField.getType());

		// Check if the path has already been reached
		if (generatorRegistry.contains(currentPath)) {
			throw new DependencyInjectionException("The data generator " + implementationClass.getSimpleName() + " already exists for the path: " 
				+ currentPath + ". It seems that you have a loop in your data generator configuration that is not allowed.");
		}
		
		// Create new instance of the data generator
		else {
			try {
				Object instanceOfImplementationClass = implementationClass.newInstance();
				generatorRegistry.add(currentPath);
				return instanceOfImplementationClass;
			}
			catch (IllegalAccessException | InstantiationException e) {
				throw new DependencyInjectionException("Unable to instantiate the data generator.", e);
			}
		}
	}

	/**
	 * Retrieve the value of a field on the holder
	 * 
	 * @param field The field from which a value must be retrieved
	 * @param holder The object where the field is
	 * @return The value of the field from the holder
	 * @throws DependencyInjectionException When it is not possible to get the value of the field from the holder
	 */
	static Object getObject(Field field, Object holder) throws DependencyInjectionException{
		// Try to inject the entity manager to the DAO.
		boolean fieldMustBeChanged = field.isAccessible();
		if (!fieldMustBeChanged) {
			field.setAccessible(true);
		}
		
		try {
			return field.get(holder);
		}
		catch (IllegalAccessException | IllegalArgumentException e) {
			throw new DependencyInjectionException(
				"Unable to get the value on field [" + field.getName() + "] of the object [" + holder.getClass().getSimpleName() + "].", e);
		}
		finally {
			if (!fieldMustBeChanged) {
				field.setAccessible(false);
			}
		}
	}
	
	/**
	 * Check if a field is null on the holder
	 * 
	 * @param field The field to check on the holder
	 * @param holder The holder where the field is
	 * @return True if the field has no value (null value), false otherwise
	 * @throws DependencyInjectionException When it is not possible to retrieve the state of the field
	 */
	static boolean isNull(Field field, Object holder) throws DependencyInjectionException {
		return getObject(field, holder) == null;
	}
}
