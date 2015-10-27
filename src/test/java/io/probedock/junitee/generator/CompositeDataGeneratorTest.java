package io.probedock.junitee.generator;

import io.probedock.junitee.generator.CompositeDataGenerator;
import io.probedock.junitee.generator.IDataGenerator;
import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the composite data generator
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@ProbeTestClass(tags = "composite-data-generator")
public class CompositeDataGeneratorTest {
	@Test
	@ProbeTest(key = "fefe06a3725d")
	public void runningCompositeDataGeneratorWithOnlyOneDataGeneratorRegisteredShouldRunCorrectly() {
		final ObjectChecker oc = new ObjectChecker();
		
		final IDataGenerator dg = new IDataGenerator() { 
			@Override public void generate() { oc.generateCalled++; oc.orderChecker += "+generate"; } 
			@Override public void cleanup() { oc.cleanupCalled++; oc.orderChecker += "+cleanup"; }
		};
		
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			@Override protected void setup() { register(dg); oc.setupCalled = true; oc.orderChecker += "+setup"; }
			@Override protected void generateData() { oc.createCalled = true; oc.orderChecker += "+create"; }
			@Override protected void cleanData() { oc.cleanCalled = true; oc.orderChecker += "+clean"; }
		};
		
		cdg.generate();
		cdg.cleanup();
		
		assertEquals("Data generator generate method should be called", 1, oc.generateCalled);
		assertEquals("Data generator cleanup method should be called", 1, oc.cleanupCalled);
		assertTrue("Composite data generator setup method should be called", oc.setupCalled);
		assertTrue("Composite data generator create method should be called", oc.createCalled);
		assertTrue("Composite data generator clean method should be called", oc.cleanCalled);
		
		assertEquals("Methods should be called in that order: setup - generate - create - clean - cleanup", "+setup+generate+create+clean+cleanup", oc.orderChecker);
	}

	@Test
	@ProbeTest(key = "470f04b03932")
	public void runningCompositeDataGeneratorWithoutDataGeneratorRegisteredShouldRunCorrectly() {
		final ObjectChecker oc = new ObjectChecker();
		
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			@Override protected void setup() { oc.setupCalled = true; oc.orderChecker += "+setup"; }
			@Override protected void generateData() { oc.createCalled = true; oc.orderChecker += "+create"; }
			@Override protected void cleanData() { oc.cleanCalled = true; oc.orderChecker += "+clean"; }
		};
		
		cdg.generate();
		cdg.cleanup();
		
		assertEquals("Data generator run method should be called", 0, oc.generateCalled);
		assertTrue("Composite data generator setup method should be called", oc.setupCalled);
		assertTrue("Composite data generator create method should be called", oc.createCalled);
		assertTrue("Composite data generator clean method should be called", oc.cleanCalled);
		
		assertEquals("Methods should be called in that order: setup - create - clean", "+setup+create+clean", oc.orderChecker);
	}

	@Test
	@ProbeTest(key = "8fff70694587")
	public void runningCompositeDataGeneratorWithThreeDataGeneratorsRegisteredShouldRunCorrectly() {
		final ObjectChecker oc = new ObjectChecker();
		
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			@Override protected void setup() { 
				oc.setupCalled = true; 
				oc.orderChecker += "+setup(beforeRegister)"; 

				register(new IDataGenerator() { 
					@Override public void generate() { oc.generateCalled++; oc.orderChecker += "+dg1g"; }
					@Override public void cleanup() { oc.cleanupCalled++; oc.orderChecker += "+dg1c"; }
				});
				register(new IDataGenerator() { 
					@Override public void generate() { oc.generateCalled++; oc.orderChecker += "+dg2g"; }
					@Override public void cleanup() { oc.cleanupCalled++; oc.orderChecker += "+dg2c"; }
				});
				register(new IDataGenerator() { 
					@Override public void generate() { oc.generateCalled++; oc.orderChecker += "+dg3g"; }
					@Override public void cleanup() { oc.cleanupCalled++; oc.orderChecker += "+dg3c"; }
				});

				oc.orderChecker += "+setup(afterRegister)"; 
			}
			
			@Override protected void generateData() { oc.createCalled = true; oc.orderChecker += "+create"; }
			@Override protected void cleanData() { oc.cleanCalled = true; oc.orderChecker += "+clean"; }
		};
		
		cdg.generate();
		cdg.cleanup();
		
		assertEquals("Data generators generate method should be called", 3, oc.generateCalled);
		assertEquals("Data generators cleanup method should be called", 3, oc.cleanupCalled);
		assertTrue("Composite data generator setup method should be called", oc.setupCalled);
		assertTrue("Composite data generator create method should be called", oc.createCalled);
		assertTrue("Composite data generator clean method should be called", oc.cleanCalled);
		
		assertEquals("Methods should be called in that order: setup - gen(dg1) - gen(dg2) - gen(dg3) - create - clean - clean(dg3) - clean(dg2) - clean(dg1)", 
			"+setup(beforeRegister)+setup(afterRegister)+dg1g+dg2g+dg3g+create+clean+dg3c+dg2c+dg1c", oc.orderChecker);
	}
	
	@Test
	@ProbeTest(key = "2a7f978a8783")
	public void overidingSetupMethodShouldNotBeMandatoryIfNoDataGeneratorIsRequired() {
		final ObjectChecker oc = new ObjectChecker();
		
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			@Override protected void generateData() { oc.createCalled = true; oc.orderChecker += "+create"; }
			@Override protected void cleanData() { oc.cleanCalled = true; oc.orderChecker += "+clean"; }
		};
		
		cdg.generate();
		cdg.cleanup();
		
		assertEquals("Data generator generate method should be called", 0, oc.generateCalled);
		assertFalse("Composite data generator setup method should not be called", oc.setupCalled);
		assertTrue("Composite data generator create method should be called", oc.createCalled);
		assertTrue("Composite data generator clean method should be called", oc.cleanCalled);
		
		assertEquals("Methods should be called in that order: create - clean", "+create+clean", oc.orderChecker);
	}

	@Test
	@ProbeTest(key = "8f67e877c063")
	public void itShouldNotBePossibleToDependsCompositeDataGeneratorOnItself() {
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			private CompositeDataGenerator internalComposite = new CompositeDataGenerator() { 
				@Override protected void generateData() {} 
				@Override protected void cleanData() {}
			};

			@Override protected void setup() { internalComposite.dependsOn(internalComposite); }
			
			@Override protected void generateData() {}
			@Override protected void cleanData() {}
		};
		
		try {
			cdg.generate();
			fail("It should not be possible to depend a data generator on itself");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("The data generator cannot depends on itself.", iae.getMessage());
		}
	}
	
	@Test
	@ProbeTest(key = "eb5b6abaafe6")
	public void itShouldNotBePossibleToDependsTwiceOnTheSameDataGenerator() {
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			private CompositeDataGenerator internalComposite = new CompositeDataGenerator() { 
				@Override protected void generateData() {} 
				@Override protected void cleanData() {}
			};

			@Override protected void setup() { 
				dependsOn("dg1", internalComposite);
				dependsOn("dg1", internalComposite);
			}
			
			@Override protected void generateData() {}
			@Override protected void cleanData() {}
		};
		
		try {
			cdg.generate();
			fail("It should not be possible to depend on the same data generator twice");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("Data generator dg1 already registered.", iae.getMessage());
		}
	}
	
	@Test
	@ProbeTest(key = "9867a0e096c2")
	public void itShouldNotBePossibleToDependsWhenNullNameIsProvided() {
		CompositeDataGenerator cdg = new CompositeDataGenerator() {
			private CompositeDataGenerator internalComposite = new CompositeDataGenerator() { 
				@Override protected void generateData() {} 
				@Override protected void cleanData() {}
			};

			@Override protected void setup() { 
				dependsOn(null, internalComposite);
			}
			
			@Override protected void generateData() {}
			@Override protected void cleanData() {}
		};
		
		try {
			cdg.generate();
			fail("It should not be possible to depend on a data generator when no name is provided");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("The data generator name should be provided.", iae.getMessage());
		}
	}
	
	@Test
	@ProbeTest(key = "7c31c81e9215")
	public void settingValuesToTheCompositeDataGeneratorConfigurationShouldAllowUsingThemIntoTheGenerateMethod() {
		CompositeDataGenerator cdg = new CompositeDataGenerator() {

			@Override
			protected void setup() {
				numberToGenerate(10).usePrefix("test");
			}

			@Override
			protected void generateData() {
				assertEquals("Number to generate should be correct", 10, getNumberToGenerate());
				assertEquals("Use prefix should be correct", "test", getPrefix());
			}
			
			@Override
			protected void cleanData() {}
		};
		
		cdg.generate();
	}

	private static class ObjectChecker {
		private boolean setupCalled = false;
		private boolean createCalled = false;
		private boolean cleanCalled = false;
		private int generateCalled = 0;
		private int cleanupCalled = 0;
		
		private String orderChecker = "";
	}
}
