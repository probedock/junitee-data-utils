package io.probedock.junitee.dependency;

import io.probedock.junitee.annotations.UseMock;
import io.probedock.junitee.dummy.ImplementationDao;
import io.probedock.junitee.dummy.DummyGeneratorWithDaos;
import io.probedock.junitee.dummy.FirstDao;
import io.probedock.junitee.dummy.GeneratorWithComplexDao;
import io.probedock.junitee.dummy.GeneratorWithInheritance;
import io.probedock.junitee.generator.IDataGenerator;
import io.probedock.junitee.annotations.InjectDataGenerator;
import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;

import org.mockito.MockitoAnnotations;

/**
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@ProbeTestClass(tags = "dependency-injector")
public class DependencyInjectorTest {
	@Mock
	private EntityManager em;
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	@ProbeTest(key = "3358d773c570")
	public void classWithImplementationDaoShouldHaveDaoInjected() {
		DummyGeneratorWithDaos injected = new DummyGeneratorWithDaos();
		
		DependencyInjector.inject(injected, em, true);
		
		assertNotNull(injected.customDao);
	}
	
	@Test
	@ProbeTest(key = "2ad8ec021f96")
	public void classWithInterfaceDaoShouldHaveDaoInjected() {
		DummyGeneratorWithDaos injected = new DummyGeneratorWithDaos();
		
		DependencyInjector.inject(injected, em, true);
		
		assertNotNull(injected.iCustomDao);
	}
	
	@Test
	@ProbeTest(key = "14787a13a5e5")
	public void anyDaoInjectedWithEmAsEntityManagerFieldShouldHaveEmNotNull() {
		DummyGeneratorWithDaos injected = new DummyGeneratorWithDaos();
		
		DependencyInjector.inject(injected, em, true);
		
		assertNotNull(injected.customDao.em);
		assertNotNull(((ImplementationDao) injected.iCustomDao).em);
	}	

	@Test
	@ProbeTest(key = "60cf490b470d")
	public void generatorThatInheritsFromAnotherGeneratorShouldHaveAllTheAnnotatedFieldsInjectedAcrossInheritanceChain() {
		GeneratorWithInheritance injected = new GeneratorWithInheritance();
		
		DependencyInjector.inject(injected, em, true);
		
		assertNotNull(injected.daoInChildClass);
		assertNotNull(injected.daoInChildClass.em);
		assertNotNull(injected.iCustomDao);
		assertNotNull(((ImplementationDao) injected.iCustomDao).em);
		assertNotNull(injected.customDao);
		assertNotNull(injected.customDao.em);
	}
	
	@Test
	@ProbeTest(key = "92021e674e90")
	public void generatorWithComplexDaoShouldHaveTheEntityManagerAndNestedEjbInjectedEveryWhere() {
		GeneratorWithComplexDao generator = new GeneratorWithComplexDao();
		
		DependencyInjector.inject(generator, em, true);
		
		assertNotNull(generator.thirdDao.abstractEm);
		assertNotNull(generator.thirdDao.thirdEm);
		assertNotNull(generator.thirdDao.firstDao.firstEm);
		assertNotNull(generator.thirdDao.secondDao.secondEm);
		assertNotNull(((FirstDao) generator.thirdDao.secondDao.firstDao).firstEm);
		assertNotNull(((FirstDao) generator.thirdDao.secondDao.firstDao).thirdDao.thirdEm);
		assertNotNull(generator.thirdDao.secondDao.secondDao.secondEm);
		assertNotNull(generator.thirdDao.secondDao.secondInstanceOfFirstDao.firstEm);
	}
	
	@Test
	@ProbeTest(key = "e9cef1d10b28")
	public void generatorWithComplexDaoThatHasCyclyReferenceShouldUseTheSameInjectedObjects() {
		GeneratorWithComplexDao generator = new GeneratorWithComplexDao();
		
		DependencyInjector.inject(generator, em, true);
		
		assertEquals(generator.thirdDao, generator.thirdDao.firstDao.thirdDao);
	}

	@Test
	@ProbeTest(key = "703196f776dd")
	public void onlyOneInstanceOfEjbShouldBeInstantiatedInComplexObjectGraph() {
		GeneratorWithComplexDao generator = new GeneratorWithComplexDao();
		
		DependencyInjector.inject(generator, em, true);
		
		assertEquals(generator.thirdDao, generator.thirdDao.firstDao.thirdDao);
		assertEquals(generator.thirdDao.firstDao, generator.thirdDao.secondDao.firstDao);
		assertEquals(generator.thirdDao.secondDao.firstDao, generator.thirdDao.secondDao.secondInstanceOfFirstDao);
		assertEquals(generator.thirdDao.secondDao, generator.thirdDao.secondDao.secondDao);
	}
	
	@Test
	@ProbeTest(key = "45e85462729a")
	public void dataGeneratorWithInternalDataGeneratorShouldBeInjectedWhenInjectorIsConfiguredForThat() {
		OutsideDataGenerator odg = new OutsideDataGenerator();
		
		DependencyInjector.inject(odg, em, true);
		
		assertNotNull("The inside data generator should be instantiated", odg.insideDataGenerator);
	}
	
	@Test
	@ProbeTest(key = "2c22fb5f2762")
	public void dataGeneratorWithInternalDataGeneratorShouldNotBeInjectedWhenInjectorIsNotConfiguredForThat() {
		OutsideDataGenerator odg = new OutsideDataGenerator();
		
		DependencyInjector.inject(odg, em, false);
		
		assertNull("The inside data generator should not be instantiated", odg.insideDataGenerator);
	}

	@Test
	@ProbeTest(key = "2a1087dd1cac")
	public void usingMockableAnnotationShouldAllowToUseMockInPlaceOfRealClass() {
		SimpleDataGenerator sdg = new SimpleDataGenerator();
		
		DependencyInjector.inject(sdg, em, false);
		
		String result = sdg.testMethod();
		
		assertEquals("Method on the mock instance should be called.", "Mock Class", result);
	}

	public static class OutsideDataGenerator {
		@InjectDataGenerator
		private InsideDataGenerator insideDataGenerator;
	}
	
	public static class InsideDataGenerator {
	}

	@Local
	public interface IMockServiceForTest { String test(); }
	public static class MockServiceForTest implements IMockServiceForTest { @Override public String test() { return "Real Class"; } }
	public static class MockServiceRealMock implements IMockServiceForTest { @Override public String test() { return "Mock Class"; } }
	
	public class SimpleDataGenerator implements IDataGenerator {
		@EJB @UseMock(MockServiceRealMock.class)
		private IMockServiceForTest localService;
		
		@Override
		public void generate() {
		}
		
		@Override
		public void cleanup() {
		}
		
		public String testMethod() {
			return localService.test();
		}
	}
}
