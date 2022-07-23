package com.github.naton1.jvmexplorer.agent;

import org.junit.Assert;
import org.junit.Test;

public class ClassFileSaveTransformerTest {

	@Test
	public void testSaveTargetClass() {
		final ClassFileSaveTransformer classFileSaveTransformer = new ClassFileSaveTransformer(String.class.getName());
		final byte[] classBytes = new byte[10];

		classFileSaveTransformer.transform(String.class.getClassLoader(),
		                                   String.class.getName(),
		                                   String.class,
		                                   null,
		                                   classBytes);

		final byte[] bytes = classFileSaveTransformer.getBytes();

		Assert.assertEquals(classBytes, bytes);
	}

	@Test
	public void testIgnoreOtherClasses() {
		final ClassFileSaveTransformer classFileSaveTransformer =
				new ClassFileSaveTransformer(Integer.class.getName());
		final byte[] classBytes = new byte[10];

		classFileSaveTransformer.transform(String.class.getClassLoader(),
		                                   String.class.getName(),
		                                   String.class,
		                                   null,
		                                   classBytes);

		final byte[] bytes = classFileSaveTransformer.getBytes();

		Assert.assertNull(bytes);
	}

}