package io.probedock.junitee.dummy;

import io.probedock.junitee.generator.IDataGenerator;
import javax.ejb.EJB;

/**
 * Generator with complex DAO to test injections
 * 
 * @author Laurent Prevost, laurent.prevost
 */
public class GeneratorWithComplexDao implements IDataGenerator {

	@EJB
	public ThirdDao thirdDao;
	
	@Override
	public void generate() {
	}
	
	@Override
	public void cleanup() {
	}
}
