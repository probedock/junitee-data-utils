package io.probedock.junitee.dummy;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Custom DAO to have the entity manager injected
 * 
 * @author Laurent Prevost
 */
public class ImplementationDao implements IImplementationDao {
	@PersistenceContext
	public EntityManager em;
}
