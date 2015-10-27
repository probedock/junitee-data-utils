package io.probedock.junitee.dependency;

import io.probedock.client.annotations.ProbeTest;
import io.probedock.client.annotations.ProbeTestClass;
import java.lang.reflect.Field;
import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 *
 * @author Laurent Prevost <laurent.prevost@probedock.io>
 */
@ProbeTestClass(tags = "dependency-inject-helper")
public class DependencyInjectorHelperTest {
	@Test
	@ProbeTest(key = "9a4eb1dd9021")
	public void fieldInjectionShouldBePossibleInAllVisibilityCasses() throws Throwable {
		Object test = new Object() {
			private Object privateField;
			protected Object protectedField;
			public Object publicField;
			Object packageField;
		};
		
		Field privateField = test.getClass().getDeclaredField("privateField");
		injectFieldAsserter("Private", privateField, test);
		
		Field protectedField = test.getClass().getDeclaredField("protectedField");
		injectFieldAsserter("Protected", protectedField, test);
		
		Field publicField = test.getClass().getDeclaredField("publicField");
		injectFieldAsserter("Public", publicField, test);
		
		Field packageField = test.getClass().getDeclaredField("packageField");
		injectFieldAsserter("Package", packageField, test);
	}
	
	private void injectFieldAsserter(String fieldType, Field field, Object holder) {
		try {
			DependencyInjectorHelper.injectField(field, holder, new Object());
		}
		catch (DependencyInjectionException die) {
			fail(fieldType + " field should be injected but was not due to: " + die.getMessage());
		}
	}
	
	@Test
	@ProbeTest(key = "cc0e9d700c99")
	public void simpleClassShouldeBeCreatedWhenCallingInstiatorHelperMethod() {
		assertNotNull("A simple class should be instantiated by the helper method.", 
			instantiationTester(SimpleClass.class));
	}
	
	@Test
	@ProbeTest(key = "20522dc30e5d")
	public void withInterfaceClassShouldBeFoundAndInstantiatedByHelperMethod() {
		assertNotNull("A simple class should be instantiated by the helper method when its interface is given.", 
			instantiationTester(ISimpleClass.class));
	}	

	@Test
	@ProbeTest(key = "0fa3e110966b")
	public void classWithoutEmptyConstructorCannotBeInstantiatedByHelperMethod() {
		assertNull("A simple class should not be instantiated by the helper method when there is no empty constructor.", 
			instantiationTester(NoEmptyConstructorClass.class));
	}
	
	@Test
	@ProbeTest(key = "618a55318822")
	public void classWithoutEmptyConstructorFromInterfaceCannotBeInstantiatedByHelperMethod() {
		assertNull("A simple class should not be instantiated by the helper method when there is no empty constructor when its interface is given.", 
			instantiationTester(INoEmptyConstructorClass.class));
	}
	
	@Test
	@ProbeTest(key = "aa4698379aff")
	public void anInterfaceShouldBeRetrievedOnlyIfRemoteAnnotationIsPresentFromClassThatImplementsIt() {
		Class interfaceClass = DependencyInjectorHelper.getMockInterface(RemoteSimpleClass.class);
		assertNotNull("An interface should be retrieved when annotated with remote", interfaceClass);
		assertEquals("The right interface should be retrieved when annotated with remote", IRemoteSimpleClass.class, interfaceClass);
	}

	@Test
	@ProbeTest(key = "8ab41b23c1a6")
	public void anInterfaceShouldBeRetrievedOnlyIfLocalAnnotationIsPresentFromClassThatImplementsIt() {
		Class interfaceClass = DependencyInjectorHelper.getMockInterface(LocalSimpleClass.class);
		assertNotNull("An interface should be retrieved when annotated with local", interfaceClass);
		assertEquals("The right interface should be retrieved when annotated with local", ILocalSimpleClass.class, interfaceClass);
	}

	@Test
	@ProbeTest(key = "c207f4b9e1ed")
	public void anInterfaceShouldNotBeRetrievedOnlyIfNoLocalOrRemoteAnnotationIsPresentFromClassThatImplementsIt() {
		Class interfaceClass = DependencyInjectorHelper.getMockInterface(SimpleClass.class);
		assertNull("An interface should not be retrieved when not annotated", interfaceClass);
	}
	
	@Test
	@ProbeTest(key = "dad6a880c7a8")
	public void postConstructShouldBeCalledWhenPresent() throws DependencyInjectionException {
		ClassWithPostConstruct cwpc = new ClassWithPostConstruct();
		
		DependencyInjectorHelper.invokePostConstruct(cwpc);
		
		assertEquals("Post construct annotated method should be called when present", "Post Construct Called", cwpc.testField);
	}
	
	private Object instantiationTester(Class cl) {
		try {
			Object obj = DependencyInjectorHelper.findImplementationClass(cl);
			System.out.println(obj);
			return obj;
		}
		catch (DependencyInjectionException die) {
			return null;
		}
	}
	
	public interface ISimpleClass {}
	public static class SimpleClass implements ISimpleClass {}

	@Remote
	public interface IRemoteSimpleClass {}
	public static class RemoteSimpleClass implements IRemoteSimpleClass {}

	@Local
	public interface ILocalSimpleClass {}
	public static class LocalSimpleClass implements ILocalSimpleClass {}

	public interface INoEmptyConstructorClass {}
	public static class NoEmptyConstructorClass implements INoEmptyConstructorClass {
		public NoEmptyConstructorClass(Object voidObject) {}
	}
	
	public static class ClassWithPostConstruct {
		public String testField;
		
		@PostConstruct
		public void postConstr() {
			testField = "Post Construct Called";
		}
	}
}
