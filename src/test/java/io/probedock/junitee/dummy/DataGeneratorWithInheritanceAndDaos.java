package io.probedock.junitee.dummy;

import javax.ejb.EJB;

/**
 * Data generator to check if the DAO is correctly instantiated
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class DataGeneratorWithInheritanceAndDaos extends DataGeneratorWithDao {
	@EJB
	public IImplementationDao secondDao;
}
