package io.probedock.junitee.dummy;

import static io.probedock.junitee.dummy.AbstractOrderGenerator.order;

/**
 * Generator TWO to test execution order
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class OrderTwoGenerator extends AbstractOrderGenerator {
	@Override
	public void generate() {
		order += "2";
	}
	
	@Override
	public void cleanup() {
		order += "3";
	}
}
