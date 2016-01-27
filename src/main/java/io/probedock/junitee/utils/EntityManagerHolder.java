package io.probedock.junitee.utils;

import io.probedock.junitee.finder.FinderManager;
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
     * Flag to ensure the holder is ready
     */
    private boolean ready = false;

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
     * @return Check if the holder is ready to use
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Build the holder to be ready to use in different managers
     *
     * @return this
     */
    public EntityManagerHolder build() {
        if (ready) {
            throw new IllegalStateException("You cannot call this method more than once.");
        }

        for (Map.Entry<String, EntityManagerFactory> e : factories.entrySet()) {
            managers.put(e.getKey(), e.getValue().createEntityManager());
        }

        ready = true;

        return this;
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
        if (ready) {
            throw new IllegalStateException("You cannot add another factory once the holder is ready to be used.");
        }

        if (DEFAULT.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("Reserved named: " + DEFAULT);
        }

        factories.put(name, factory);

        return this;
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

        if (entityManagerName != null && !DEFAULT.equalsIgnoreCase(entityManagerName.value())) {
            return managers.get(entityManagerName.value());
        }
        else {
            return managers.get(DEFAULT);
        }
    }

    /**
     * @return The collection of managers
     */
    public Collection<EntityManager> getManagers() {
        return managers.values();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Factories: [");

        for (Map.Entry<String, EntityManagerFactory> e : factories.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append(", ");
        }

        sb.append("], Managers: [");

        for (Map.Entry<String, EntityManager> e : managers.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append(", ");
        }

        sb.append("]");

        return sb.toString();
    }
}
