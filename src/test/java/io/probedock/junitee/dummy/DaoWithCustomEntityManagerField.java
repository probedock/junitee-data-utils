package io.probedock.junitee.dummy;

import javax.persistence.EntityManager;

/**
 * This DAO allow testing that it is possible to inject DAO with
 * an entity manager field name that is not "em"
 * 
 * @author Laurent Prevost
 */
public class DaoWithCustomEntityManagerField implements IImplementationDao {
	public EntityManager entityManager;
}
