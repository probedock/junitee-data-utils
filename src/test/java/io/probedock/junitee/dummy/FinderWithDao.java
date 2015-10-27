package io.probedock.junitee.dummy;

import io.probedock.junitee.finder.IFinder;
import javax.ejb.EJB;

/**
 * Data generator to check that the before and after are called
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class FinderWithDao implements IFinder {
	@EJB
	public IImplementationDao iCustomDao;
	
	@EJB
	public ImplementationDao customDao;
	
	public void find() {}
}
