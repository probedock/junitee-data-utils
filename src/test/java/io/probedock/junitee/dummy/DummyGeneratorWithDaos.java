package io.probedock.junitee.dummy;

import javax.ejb.EJB;

/**
 * Custom data generator to have DAO injected
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DummyGeneratorWithDaos {
	@EJB
	public IImplementationDao iCustomDao;
	
	@EJB
	public ImplementationDao customDao;
}