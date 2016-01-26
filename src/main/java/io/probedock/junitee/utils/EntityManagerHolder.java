package io.probedock.junitee.utils;

import io.probedock.junitee.finder.IFinder;
import io.probedock.junitee.annotations.EntityManagerName;
import io.probedock.junitee.generator.IDataGenerator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class allows the registration of the entity manager factories when a rule is created. Based on the rules registered
 * the data generator will use them to create the entity managers and apply them in the object graph of the generators and
 * also during the transaction management.
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
public class EntityManagerHolder {
    private static final String DEFAULT = "_DEFAULT_";

    /**
     * The entity manager factories storage
     */
    private final Map<String, EntityManagerFactory> factories = new HashMap<>();

    /**
     * The entity managers
     */
    private final Map<String, EntityManager> managers = new HashMap<>();

    /**
     * Flag to only build once
     */
    private boolean built = false;

    /**
     * Constructor
     *
     * @param defaultFactory The default factory is mandatory
     */
    public EntityManagerHolder(EntityManagerFactory defaultFactory) {
        if (defaultFactory == null) {
            throw new IllegalArgumentException("The default factory cannot be null.");
        }

        factories.put(DEFAULT, defaultFactory);
    }

    /**
     * Build the holder to be ready to use in different managers
     */
    public synchronized void build() {
        if (!built) {
            for (Map.Entry<String, EntityManagerFactory> e : factories.entrySet()) {
                managers.put(e.getKey(), e.getValue().createEntityManager());
            }
            built = true;
        }
    }

    /**
     * Add a new factory to the config. If name already used, the new factory will override the previous one.
     * The name can be null.
     *
     * @param name Name of the factory
     * @param factory The factory
     * @return This
     */
    public EntityManagerHolder addFactory(String name, EntityManagerFactory factory) {
        if (DEFAULT.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Reserved named: " + DEFAULT);
        }

        factories.put(name, factory);

        return this;
    }

    /**
     * @return Retrieve the default manager
     */
    public EntityManager getDefaultManager() {
        return managers.get(DEFAULT);
    }

    /**
     * @param name The name of the factory
     * @return The factory or null if not present
     */
    public EntityManager getManager(String name) {
        // Cannot retrieve the default factory through this getter
        if (DEFAULT.equalsIgnoreCase(name)) {
            return null;
        }

        return managers.get(name);
    }

    /**
     * Retrieve an entity manager corresponding to the data generator
     *
     * @param dgClass The data generator class
     * @return The corresponding entity manager
     */
    public EntityManager retrieveEntityManagerFromDataGenerator(Class<? extends IDataGenerator> dgClass) {
        return retrieveEntityManager(dgClass);
    }

    /**
     * Retrieve an entity manager corresponding to the finder
     *
     * @param fClass The finder class
     * @return The corresponding entity manager
     */
    public EntityManager retrieveEntityManagerFromFinder(Class<? extends IFinder> fClass) {
        return retrieveEntityManager(fClass);
    }

    /**
     * Retrieve the entity manager corresponding to the data manager
     *
     * @param cl The data generator class
     * @return The corresponding entity manager
     */
    private EntityManager retrieveEntityManager(Class<?> cl) {
        EntityManagerName entityManagerName = cl.getAnnotation(EntityManagerName.class);

        if (entityManagerName != null) {
            return getManager(entityManagerName.value());
        }
        else {
            return getDefaultManager();
        }
    }

    /**
     * @return The collection of managers
     */
    public Collection<EntityManager> getManagers() {
        return managers.values();
    }
}
