package io.probedock.junitee.finder;

import io.probedock.junitee.annotations.Finder;
import io.probedock.junitee.dummy.DummyFinder;
import io.probedock.junitee.dummy.FinderWithDao;
import io.probedock.junitee.dummy.FinderWithInheritanceAndDaos;
import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import java.lang.annotation.Annotation;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import io.probedock.junitee.utils.EntityManagerHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@ProbeTestClass(tags = "finder-manager")
public class FinderManagerTest {
	@Mock
	private Statement statement;
	
	@Mock EntityManagerFactory entityManagerFactory;

	private EntityManagerHolder entityManagerHolder;

	@Mock
	private EntityManager entityManager;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);

		entityManagerHolder = new EntityManagerHolder(entityManagerFactory).build();
	}

	@Test
	@ProbeTest(key = "8bed5e1bdf4f")
	@SuppressWarnings("unchecked")
	public void finderForGivenDescrptionShouldBeAvailableIntoTheTest() throws Throwable {
		Finder annotation = new Finder() {
			@Override
			public Class<? extends IFinder>[] value() {
				return new Class[] { DummyFinder.class };
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Finder.class;
			}
		};
		
		Description description = Description.createSuiteDescription("Some description", annotation);
		
		FinderManager fm = new FinderManager(entityManagerHolder);
		fm.apply(statement, description).evaluate();

		assertNotNull(fm.getFinder(DummyFinder.class));
	}

	@Test
	@ProbeTest(key = "6897d7617dec")
	@SuppressWarnings("unchecked")
	public void registeringTwoFindersOfTheSameTypeShouldBeForbiden() throws Throwable {
		Finder annotation = new Finder() {
			@Override
			public Class<? extends IFinder>[] value() {
				return new Class[] { DummyFinder.class, DummyFinder.class };
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Finder.class;
			}
		};
		
		Description description = Description.createSuiteDescription("Some description", annotation);
		FinderManager fm = new FinderManager(entityManagerHolder);
		
		try {
			fm.apply(statement, description).evaluate();
			fail("The finder exception should be raised to prevent registering two finders of the same type.");
		}
		catch (FinderException fe) { /* Do nothing */ }
	}

	@Test
	@ProbeTest(key = "2ba81197832e")
	@SuppressWarnings("unchecked")
	public void finderWithDaoShouldHaveTheDaoNotNullButDaoAreInstantiatedOnlyLazily() throws Throwable {
		Finder annotation = new Finder() {
			@Override
			public Class<? extends IFinder>[] value() {
				return new Class[] { FinderWithDao.class };
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Finder.class;
			}
		};
		
		Description description = Description.createSuiteDescription("Some description", annotation);
		FinderManager fm = new FinderManager(entityManagerHolder);
		fm.apply(statement, description).evaluate();

		assertNull(fm.getFinder(FinderWithDao.class).customDao);
		assertNull(fm.getFinder(FinderWithDao.class).iCustomDao);
		
		fm.getFinder(FinderWithDao.class).find();
		
		assertNotNull(fm.getFinder(FinderWithDao.class).customDao);
		assertNotNull(fm.getFinder(FinderWithDao.class).iCustomDao);
		
	}

	@Test
	@ProbeTest(key = "be1b9b84fd34")
	@SuppressWarnings("unchecked")
	public void inheritedFinderWithDaoShouldHaveAllInheritedDaoNotNullButDaoAreInstantiatedOnlyLazily() throws Throwable {
		Finder annotation = new Finder() {
			@Override
			public Class<? extends IFinder>[] value() {
				return new Class[] { FinderWithInheritanceAndDaos.class };
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Finder.class;
			}
		};
		
		Description description = Description.createSuiteDescription("Some description", annotation);
		FinderManager fm = new FinderManager(entityManagerHolder);
		fm.apply(statement, description).evaluate();

		assertNotNull(fm.getFinder(FinderWithInheritanceAndDaos.class));
		
		assertNull(fm.getFinder(FinderWithInheritanceAndDaos.class).customDao);
		assertNull(fm.getFinder(FinderWithInheritanceAndDaos.class).iCustomDao);
		assertNull(fm.getFinder(FinderWithInheritanceAndDaos.class).childCustomDao);
		assertNull(fm.getFinder(FinderWithInheritanceAndDaos.class).childICustomDao);
		
		fm.getFinder(FinderWithInheritanceAndDaos.class).find();
		
		assertNotNull(fm.getFinder(FinderWithInheritanceAndDaos.class).customDao);
		assertNotNull(fm.getFinder(FinderWithInheritanceAndDaos.class).iCustomDao);
		assertNotNull(fm.getFinder(FinderWithInheritanceAndDaos.class).childCustomDao);
		assertNotNull(fm.getFinder(FinderWithInheritanceAndDaos.class).childICustomDao);
	}
}
