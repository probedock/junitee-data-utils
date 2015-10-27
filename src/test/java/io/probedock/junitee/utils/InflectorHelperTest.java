package io.probedock.junitee.utils;

import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@ProbeTestClass(tags = "inflector-helper")
public class InflectorHelperTest {
	public interface ICustomDao {}
	
	public static class CustomDao implements ICustomDao {}
	
	@Test
	@ProbeTest(key = "4cad7922ddfe")
	public void itShouldBePossibleToGetImplementationClassForGivenInterfaceThatFollowsCodingConventionNames() throws Throwable {
		assertEquals(CustomDao.class, InflectorHelper.retrieveInstantiableClassName(ICustomDao.class));
		assertEquals(SeparateClass.class, InflectorHelper.retrieveInstantiableClassName(ISeparateClass.class));
		assertEquals(NamingWithImplImpl.class, InflectorHelper.retrieveInstantiableClassName(NamingWithImpl.class));
	}
	
	@Test
	@ProbeTest(key = "827e479e1ce7")
	public void whenImplementationClassIsGivenTheSameClassShouldBeRetrieved() throws Throwable {
		assertEquals(CustomDao.class, InflectorHelper.retrieveInstantiableClassName(CustomDao.class));
		assertEquals(SeparateClass.class, InflectorHelper.retrieveInstantiableClassName(SeparateClass.class));
	}
	
	@Test
	@ProbeTest(key = "3b7dd823543f")
	public void nonStaticPublicClassesCannotBeUsed() {
		noValidClassAsserter(PrivateStaticClass.class);
		noValidClassAsserter(PrivateClass.class);
		
		noValidClassAsserter(ProtectedStaticClass.class);
		noValidClassAsserter(ProtetectedClass.class);
		
		noValidClassAsserter(PackageStaticClass.class);
		noValidClassAsserter(PackageClass.class);
		
		noValidClassAsserter(PublicClass.class);
	}
	
	private void noValidClassAsserter(Class cl) {
		try { 
			InflectorHelper.retrieveInstantiableClassName(cl); 
			fail("The " + cl.getCanonicalName() + " should not be a valid class for the inflector.");
		} catch (NoValidClassException nvce) {}
	}
	
	private static class PrivateStaticClass {}
	private class PrivateClass {}
	
	protected static class ProtectedStaticClass {}
	protected class ProtetectedClass {}
	
	static class PackageStaticClass {}
	class PackageClass {}
	
	public class PublicClass {}
}
