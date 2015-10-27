package io.probedock.junitee.dummy;

import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Abstract DAO to test injection accros all the levels
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class AbstractDao {
	@PersistenceContext
	public EntityManager abstractEm;
	
	@EJB
	public FirstDao firstDao;
}
