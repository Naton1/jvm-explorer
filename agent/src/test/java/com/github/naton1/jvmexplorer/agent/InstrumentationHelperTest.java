package com.github.naton1.jvmexplorer.agent;

import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import lombok.Value;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;

public class InstrumentationHelperTest {

	private InstrumentationHelper instrumentationHelper;
	private Instrumentation instrumentation;

	@Before
	public void setup() {
		instrumentation = Mockito.mock(Instrumentation.class);
		instrumentationHelper = new InstrumentationHelper(instrumentation);
	}

	@Test
	public void testGetApplicationClasses() {
		final Class<?>[] classes = { int.class,
		                             AgentFileLogger.class,
		                             Long[].class,
		                             Long.class,
		                             String.class,
		                             ClientListener[].class };

		Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(classes);
		Mockito.when(instrumentation.isModifiableClass(ArgumentMatchers.any(Class.class)))
		       .thenAnswer(new Answer<Boolean>() {
			       @Override
			       public Boolean answer(final InvocationOnMock invocation) {
				       // We'll say Long is not modifiable
				       return invocation.getArgument(0, Class.class) != Long.class;
			       }
		       });

		final List<Class<?>> applicationClasses = instrumentationHelper.getApplicationClasses();
		Assert.assertEquals(Collections.singletonList(String.class), applicationClasses);
	}

	@Test
	public void testGetClassBytes() throws UnmodifiableClassException {
		final byte[] classBytes = new byte[10];
		final ClassFileSaveTransformer[] classFileSaveTransformer = new ClassFileSaveTransformer[1];
		final Class<?> targetClass = String.class;
		Mockito.doAnswer(new Answer<Void>() {
			       @Override
			       public Void answer(final InvocationOnMock invocation) {
				       classFileSaveTransformer[0] = invocation.getArgument(0, ClassFileSaveTransformer.class);
				       return null;
			       }
		       })
		       .when(instrumentation)
		       .addTransformer(ArgumentMatchers.any(ClassFileTransformer.class), ArgumentMatchers.anyBoolean());
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(final InvocationOnMock invocation) {
				classFileSaveTransformer[0].transform(targetClass.getClassLoader(),
				                                      targetClass.getName(),
				                                      targetClass,
				                                      targetClass.getProtectionDomain(),
				                                      classBytes);
				return null;
			}
		}).when(instrumentation).retransformClasses(ArgumentMatchers.any(Class.class));

		final byte[] classBytesResult = instrumentationHelper.getClassBytes(targetClass);

		Assert.assertEquals(classBytes, classBytesResult);
	}

	@Test
	public void testGetClassByName() {
		final Class<?> klass = instrumentationHelper.getClassByName("java.lang.String");
		Assert.assertEquals(String.class, klass);
	}

	@Test
	public void testGetClassByNameWithClassLoader() {
		final Class<?> klass = instrumentationHelper.getClassByName(InstrumentationHelperTest.class.getName(),
		                                                            InstrumentationHelperTest.class.getClassLoader());
		Assert.assertEquals(InstrumentationHelperTest.class, klass);
	}

	@Test
	public void givenTwoClassesWithSameNameButDifferentClassLoader_whenGetClassByName_thenCorrectClassReturned()
			throws ClassNotFoundException {
		final URL location = TestStaticClass.class.getProtectionDomain().getCodeSource().getLocation();
		final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { location }) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) {
				// Force it to load a new class rather than reuse the already loaded one
				try {
					if (name.equals(TestStaticClass.class.getName())) {
						return super.findClass(name);
					}
					return super.loadClass(name, resolve);
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
					return null;
				}
			}
		};
		urlClassLoader.loadClass(TestStaticClass.class.getName());
		final Class<?> klass = instrumentationHelper.getClassByName(TestStaticClass.class.getName(), urlClassLoader);
		final Class<?> otherClassWithSameName = TestStaticClass.class;
		Assert.assertNotEquals(klass, otherClassWithSameName);
	}

	@Test
	public void testSetObject() {
		final ClassFieldKey[] classFieldKeys = { new ClassFieldKey(TestStaticClass.class.getName(),
		                                                           "instance",
		                                                           TestStaticClass.class.getName(),
		                                                           Modifier.PUBLIC),
		                                         new ClassFieldKey(TestStaticClass.class.getName(),
		                                                           "name",
		                                                           String.class.getName(),
		                                                           Modifier.PRIVATE) };
		final String newValue = "test";

		TestStaticClass.instance = new TestStaticClass("name");
		instrumentationHelper.setObject(TestStaticClass.class.getClassLoader(), classFieldKeys, newValue);

		Assert.assertEquals(newValue, TestStaticClass.instance.name);
		TestStaticClass.instance = null;
	}

	@Test
	public void testGetClassFieldsByPath() {
		final ClassFieldKey[] classFieldKeys = { new ClassFieldKey(TestStaticClass.class.getName(),
		                                                           "instance",
		                                                           TestStaticClass.class.getName(),
		                                                           Modifier.PUBLIC) };
		final String name = "test";

		TestStaticClass.instance = new TestStaticClass(name);
		final ClassFields classFields = instrumentationHelper.getClassFields(TestStaticClass.class.getClassLoader(),
		                                                                     classFieldKeys);
		// class field 0 is the static instance, 1 is name
		final String nameValue = (String) classFields.getFields()[1].getValue();

		Assert.assertEquals(name, nameValue);
		TestStaticClass.instance = null;
	}

	@Test
	public void testGetObject() {
		final ClassFieldKey[] classFieldKeys = { new ClassFieldKey(TestStaticClass.class.getName(),
		                                                           "instance",
		                                                           TestStaticClass.class.getName(),
		                                                           Modifier.PUBLIC),
		                                         new ClassFieldKey(TestStaticClass.class.getName(),
		                                                           "name",
		                                                           String.class.getName(),
		                                                           Modifier.PRIVATE) };

		TestStaticClass.instance = new TestStaticClass("name");
		final String name = (String) instrumentationHelper.getObject(TestStaticClass.class.getClassLoader(),
		                                                             classFieldKeys);

		Assert.assertEquals(TestStaticClass.instance.name, name);
		TestStaticClass.instance = null;
	}

	@Test
	public void testGetClassFields() {
		final String name = "test";

		TestStaticClass.instance = new TestStaticClass(name);
		final ClassFields classFields = instrumentationHelper.getClassFields(TestStaticClass.class,
		                                                                     TestStaticClass.instance);
		// class field 0 is the static instance, 1 is name
		final String nameValue = (String) classFields.getFields()[1].getValue();

		Assert.assertEquals(name, nameValue);
		TestStaticClass.instance = null;
	}

	@Test
	public void testRedefineClass() throws UnmodifiableClassException, ClassNotFoundException {
		final Class<?> redefineClass = String.class;
		final byte[] newClassBytes = new byte[10];

		instrumentationHelper.redefineClass(redefineClass, newClassBytes);

		final ArgumentCaptor<ClassDefinition> classDefinitionCaptor = ArgumentCaptor.forClass(ClassDefinition.class);
		Mockito.verify(instrumentation, Mockito.times(1)).redefineClasses(classDefinitionCaptor.capture());

		final ClassDefinition classDefinition = classDefinitionCaptor.getValue();
		Assert.assertEquals(redefineClass, classDefinition.getDefinitionClass());
		Assert.assertEquals(newClassBytes, classDefinition.getDefinitionClassFile());
	}

	@Value
	public static class TestStaticClass {
		public static TestStaticClass instance;
		private final String name;
	}

}