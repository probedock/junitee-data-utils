package io.probedock.junitee.dummy;

/**
 * Generator ONE to test execution order
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class OrderOneGenerator extends AbstractOrderGenerator {
	@Override
	public void generate() {
		order += "1";
	}
	
	@Override
	public void cleanup() {
		order += "4";
	}
}
