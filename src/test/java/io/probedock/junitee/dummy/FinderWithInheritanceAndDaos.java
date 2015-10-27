package io.probedock.junitee.dummy;

import javax.ejb.EJB;

/**
 * Data generator to check that the before and after are called
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class FinderWithInheritanceAndDaos extends FinderWithDao {
	@EJB
	public IImplementationDao childICustomDao;
	
	@EJB
	public ImplementationDao childCustomDao;
}
