package io.probedock.junitee.dummy;

import io.probedock.junitee.generator.IDataGenerator;
import javax.ejb.EJB;

/**
 * Data generator to check if the DAO is correctly instantiated
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DataGeneratorWithDao implements IDataGenerator {
	@EJB
	public IImplementationDao firstDao;
	
	@Override
	public void generate() {
	}
	
	@Override
	public void cleanup() {
	}
	
	public void createSomething() {}
	public Object createSomethingElse() { return new Object(); }

	public void deleteSomething() {}
	public Object deleteSomethingElse() { return new Object(); }

	public void updateSomething() {}
	public Object updateSomethingElse() { return new Object(); }
}
