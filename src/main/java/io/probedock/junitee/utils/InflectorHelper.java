package io.probedock.junitee.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Helper class to extract class names
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public final class InflectorHelper {
	private InflectorHelper() {}
	
	public static Class retrieveInstantiableClassName(Class cl) throws NoValidClassException {
		Class implementationClass = cl;
		
		String packageName = cl.getPackage().getName();
		
		// By convention, if we have an interface, there is a prefix of one character
		if (cl.isInterface()) {
			String simpleName;
			if (cl.getSimpleName().charAt(0) == 'I' && Character.isUpperCase(cl.getSimpleName().charAt(1))) {
				simpleName = cl.getSimpleName().substring(1);
			}
			else {
				simpleName = cl.getSimpleName() + "Impl";
			}
			
			// Try to load the implementation class of the interface
			try {
				if (cl.isMemberClass()) {
					implementationClass = InflectorHelper.class.getClassLoader().loadClass(packageName + "." + cl.getDeclaringClass().getSimpleName() + "$" + simpleName);
				}
				else {
					implementationClass = InflectorHelper.class.getClassLoader().loadClass(packageName + "." + simpleName);
				}
			}
			catch (ClassNotFoundException cnfe) {
				throw new NoValidClassException("Unable to find a concrete class for interface " + cl.getCanonicalName());
			}
		}

		// Check that the implementation class is public
		if (!Modifier.isPublic(implementationClass.getModifiers())) {
			throw new NoValidClassException("The class " + implementationClass.getCanonicalName() + " is not public.");
		}
		
		// Check if it is an internal class
		if (implementationClass.isMemberClass()) {		
			// Check if it is a valid internal class
			if (!Modifier.isStatic(implementationClass.getModifiers())) {
				throw new NoValidClassException("The inner class should be static to be instantiated through junitee.");
			}
		}
		
		try {
			@SuppressWarnings("unchecked")
			Constructor constructor = implementationClass.getConstructor();
			if (!Modifier.isPublic(constructor.getModifiers())) {
				throw new NoValidClassException("The empty constructor of class " + implementationClass.getSimpleName() + " must be public.");
			}
		}
		catch (NoSuchMethodException | SecurityException e) {
			throw new NoValidClassException("It seems that " + implementationClass.getSimpleName() + " has no empty constructor.");
		}
		
		return implementationClass;
	}
}
