package io.probedock.junitee.dummy;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A second DAO to build a chain of DAO
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class SecondDao {
	@PersistenceContext
	public EntityManager secondEm;
	
	@EJB
	public IFirstDao firstDao;
	
	@EJB
	public SecondDao secondDao;
	
	@EJB
	public FirstDao secondInstanceOfFirstDao;
}
