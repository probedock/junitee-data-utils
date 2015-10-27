package io.probedock.junitee.utils;

/**
 * Exception when a class is not valid to be instantiated within junitee
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class NoValidClassException extends Exception {
	public NoValidClassException(String message) {
		super(message);
	}
}
