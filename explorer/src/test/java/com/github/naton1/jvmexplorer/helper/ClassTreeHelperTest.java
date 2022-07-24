package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.fx.classes.PackageTreeNode;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

class ClassTreeHelperTest {

	@Test
	void testBuildClassLoaderTree() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();
		final ClassLoaderDescriptor parentClassLoaderDescriptor = ClassLoaderDescriptor.builder()
		                                                                               .description("test1")
		                                                                               .id("1")
		                                                                               .simpleClassName("ClassLoader")
		                                                                               .parent(null)
		                                                                               .build();
		final ClassLoaderDescriptor classLoaderDescriptor = ClassLoaderDescriptor.builder()
		                                                                         .description("test2")
		                                                                         .id("2")
		                                                                         .simpleClassName("ClassLoader")
		                                                                         .parent(parentClassLoaderDescriptor)
		                                                                         .build();
		final LoadedClass loadedClass = new LoadedClass("test.TestClass", classLoaderDescriptor);
		final LoadedClass otherClass = new LoadedClass("test.something.OtherClass", classLoaderDescriptor);
		final LoadedClass rootClass = new LoadedClass("test.Root", parentClassLoaderDescriptor);

		final PackageTreeNode expectedRoot = PackageTreeNode.root();
		final PackageTreeNode expectedParentClassLoader = expectedRoot.addClassLoader(parentClassLoaderDescriptor);
		final PackageTreeNode expectedClassLoader = expectedParentClassLoader.addClassLoader(classLoaderDescriptor);
		final PackageTreeNode expectedTestPackage = expectedClassLoader.addPackage("test");
		expectedTestPackage.addClass(loadedClass);
		expectedTestPackage.addPackage("something").addClass(otherClass);
		expectedParentClassLoader.addPackage("test").addClass(rootClass);

		final PackageTreeNode root = classTreeHelper.buildClassLoaderTree(List.of(loadedClass, otherClass, rootClass));

		Assertions.assertEquals(expectedRoot,
		                        root,
		                        expectedRoot.toDetailedString() + "\n!=\n" + root.toDetailedString() + "\n");
	}

	@Test
	void testBuildClassTree() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();
		final ClassLoaderDescriptor parentClassLoaderDescriptor = ClassLoaderDescriptor.builder()
		                                                                               .description("test1")
		                                                                               .id("1")
		                                                                               .simpleClassName("ClassLoader")
		                                                                               .parent(null)
		                                                                               .build();
		final ClassLoaderDescriptor classLoaderDescriptor = ClassLoaderDescriptor.builder()
		                                                                         .description("test2")
		                                                                         .id("2")
		                                                                         .simpleClassName("ClassLoader")
		                                                                         .parent(parentClassLoaderDescriptor)
		                                                                         .build();
		final LoadedClass loadedClass = new LoadedClass("test.TestClass", classLoaderDescriptor);
		final LoadedClass otherClass = new LoadedClass("test.something.OtherClass", classLoaderDescriptor);
		final LoadedClass rootClass = new LoadedClass("test.Root", parentClassLoaderDescriptor);

		final PackageTreeNode expectedRoot = PackageTreeNode.root();
		final PackageTreeNode expectedTestPackage = expectedRoot.addPackage("test");
		expectedTestPackage.addClass(loadedClass);
		expectedTestPackage.addPackage("something").addClass(otherClass);
		expectedTestPackage.addClass(rootClass);

		final PackageTreeNode root = classTreeHelper.buildClassTree(List.of(loadedClass, otherClass, rootClass));

		Assertions.assertEquals(expectedRoot,
		                        root,
		                        expectedRoot.toDetailedString() + "\n!=\n" +  root.toDetailedString() + "\n");
	}

}