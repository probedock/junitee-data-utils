package io.probedock.junitee.dependency;

/**
 * Exception in case of injection failures
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DependencyInjectionException extends Exception {
	public DependencyInjectionException(String message) {
		super(message);
	}
	
	public DependencyInjectionException(String message, Throwable cause) {
		super(message, cause);
	}
}
