package io.probedock.junitee.dummy;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * A first DAO to build a chain of DAO
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class FirstDao implements IFirstDao {
	@PersistenceContext
	public EntityManager firstEm;
	
	@EJB
	public ThirdDao thirdDao;
}
