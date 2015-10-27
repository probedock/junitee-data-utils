package io.probedock.junitee.generator;

import io.probedock.junitee.generator.DataGeneratorRunner;
import io.probedock.junitee.generator.IDataGenerator;
import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test class for the complext data generator
 * 
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@ProbeTestClass(tags = "data-generator-runner")
public class DataGeneratorRunnerTest {
	@Test
	@ProbeTest(key = "da0a866d3b98")
	public void creatingAndRunningDataGeneratorRunnerWithOneDataGeneratorShouldWork() {
		final ObjectChecker oc = new ObjectChecker();
		
		IDataGenerator dg = new IDataGenerator() { 
			@Override public void generate() { oc.generateCount++; } 
			@Override public void cleanup() { oc.cleanupCount++; } 
		};

		DataGeneratorRunner dgr = new DataGeneratorRunner(dg);

		assertNotNull("Data generator should be registered in the runner", dgr.getDataGenerator());

		dgr.generate();
		dgr.cleanup();
		
		assertEquals("Only one runs should be done on data generator runner", 1, oc.generateCount);
		assertEquals("Only one runs should be done on data generator runner", 1, oc.cleanupCount);
	}
	
	@Test
	@ProbeTest(key = "c6be936166d1")
	public void nextDataGeneratorRunnerShouldBeNullWhenCreatingDataGeneratorRunnerWithOneDataGenerator() {
		IDataGenerator dg = new IDataGenerator() { 
			@Override public void generate() {} 
			@Override public void cleanup() {}
		};

		DataGeneratorRunner dgr = new DataGeneratorRunner(dg);
		
		assertNull("No next runner should be available", dgr.getNext());
	}
	
	@Test
	@ProbeTest(key = "3cde9a9a97ca")
	public void creatingAndRunningDataGeneratorRunnerWithTwoDataGeneratorsShouldWork() {
		final ObjectChecker oc = new ObjectChecker();
		
		IDataGenerator dg1 = new IDataGenerator() { 
			@Override public void generate() { oc.generateCount++; } 
			@Override public void cleanup() { oc.cleanupCount++; } 
		};

		DataGeneratorRunner dgr1 = new DataGeneratorRunner(dg1);

		assertNotNull("First data generator should be registered in the runner", dgr1.getDataGenerator());
		
		IDataGenerator dg2 = new IDataGenerator() { 
			@Override public void generate() { oc.generateCount++; } 
			@Override public void cleanup() { oc.cleanupCount++; } 
		};
		
		DataGeneratorRunner dgr2 = dgr1.setNext(dg2);
		
		assertNotNull("Second data generator should be registered in the runner", dgr2);
		assertNotEquals("Second data generator runner should not be the same as the first one", dgr1, dgr2);

		dgr2.generate();
		
		assertEquals("Generate on the second generator runner should only run the second generator", 1, oc.generateCount);
		
		dgr1.generate();

		assertEquals("Generate on the first generator runner should run all the generators", 3, oc.generateCount);

		dgr1.cleanup();
		
		assertEquals("Cleanup on the first generator runner should only run first generator", 1, oc.cleanupCount);

		dgr2.cleanup();

		assertEquals("Cleanup on the second generator runner should run all the geenrators", 3, oc.cleanupCount);
		
	}
	
	@Test
	@ProbeTest(key = "97a3ef75e710")
	public void creatingDataGeneratorRunnerChainShouldKeepTheCorrectOrderInDataStructureAndRun() {
		final ObjectChecker oc = new ObjectChecker();
		
		IDataGenerator dg1 = new IDataGenerator() { 
			@Override public void generate() { oc.stringChecker += "+dg1g"; } 
			@Override public void cleanup() { oc.stringChecker += "+dg1c"; } 
		};
		IDataGenerator dg2 = new IDataGenerator() { 
			@Override public void generate() { oc.stringChecker += "+dg2g"; } 
			@Override public void cleanup() { oc.stringChecker += "+dg2c"; } 
		};
		IDataGenerator dg3 = new IDataGenerator() { 
			@Override public void generate() { oc.stringChecker += "+dg3g"; } 
			@Override public void cleanup() { oc.stringChecker += "+dg3c"; } 
		};

		DataGeneratorRunner dgr = new DataGeneratorRunner(dg1);

		dgr.setNext(dg2).setNext(dg3);

		assertEquals("First data generator should be dg1", dg1, dgr.getDataGenerator());
		assertEquals("Second data generator should be dg2", dg2, dgr.getNext().getDataGenerator());
		assertEquals("Third data generator should be dg3", dg3, dgr.getNext().getNext().getDataGenerator());
		assertNull("No forth data generator should be present", dgr.getNext().getNext().getNext());
		
		assertEquals("Datagenerator before dg3 should be dg2", dg2, dgr.getNext().getNext().getPrevious().getDataGenerator());
		assertEquals("Datagenerator before dg2 should be dg1", dg1, dgr.getNext().getNext().getPrevious().getPrevious().getDataGenerator());
		assertNull("There is no dataganerator before dg1", dgr.getNext().getNext().getPrevious().getPrevious().getPrevious());

		dgr.generate();
		dgr.getNext().getNext().cleanup();
		
		assertEquals("Executing data generator runner should respect the order", "+dg1g+dg2g+dg3g+dg3c+dg2c+dg1c", oc.stringChecker);
	}
	
	private static class ObjectChecker {
		private int generateCount = 0;
		private int cleanupCount = 0;
		private String stringChecker = "";
	}
}
