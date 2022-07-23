package com.github.naton1.jvmexplorer.protocol.helper;

import org.junit.Assert;
import org.junit.Test;

public class ClassNameHelperTest {

	@Test
	public void testSimpleNameWithPackage() {
		final String className = "test.some.name.MyClass";
		final String simpleName = ClassNameHelper.getSimpleName(className);
		Assert.assertEquals("MyClass", simpleName);
	}

	@Test
	public void testSimpleNameNoPackage() {
		final String className = "MyClass";
		final String simpleName = ClassNameHelper.getSimpleName(className);
		Assert.assertEquals(className, simpleName);
	}

	@Test
	public void testSimpleNameEmptyString() {
		final String className = "";
		final String simpleName = ClassNameHelper.getSimpleName(className);
		Assert.assertEquals(className, simpleName);
	}

	@Test
	public void testSimpleNameDotLastChar() {
		final String className = "invalid-class-name.";
		final String simpleName = ClassNameHelper.getSimpleName(className);
		Assert.assertEquals("", simpleName);
	}

}