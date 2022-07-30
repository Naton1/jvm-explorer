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

	@Test
	public void testPackageName() {
		final String packageName = ClassNameHelper.getPackageName("my.c.MyClass");
		Assert.assertEquals("my.c", packageName);
	}

	@Test
	public void testEmptyPackage() {
		final String packageName = ClassNameHelper.getPackageName("MyClass");
		Assert.assertEquals("", packageName);
	}

	@Test
	public void testEmptyString() {
		final String packageName = ClassNameHelper.getPackageName("");
		Assert.assertEquals("", packageName);
	}

	@Test
	public void testPackageEndsWithDot() {
		final String packageName = ClassNameHelper.getPackageName("mypackage.");
		Assert.assertEquals("mypackage", packageName);
	}

}