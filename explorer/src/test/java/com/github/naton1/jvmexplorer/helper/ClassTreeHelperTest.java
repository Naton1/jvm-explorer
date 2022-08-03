package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.fx.classes.ClassTreeNode;
import com.github.naton1.jvmexplorer.fx.classes.FilterableTreeItem;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

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
		final LoadedClass loadedClass = new LoadedClass("test.TestClass", classLoaderDescriptor, null);
		final LoadedClass otherClass = new LoadedClass("test.something.OtherClass", classLoaderDescriptor, null);
		final LoadedClass rootClass = new LoadedClass("test.Root", parentClassLoaderDescriptor, null);

		final ClassTreeNode expectedRoot = ClassTreeNode.root();
		final ClassTreeNode expectedParentClassLoader = expectedRoot.addClassLoader(parentClassLoaderDescriptor);
		final ClassTreeNode expectedClassLoader = expectedParentClassLoader.addClassLoader(classLoaderDescriptor);
		final ClassTreeNode expectedTestPackage = expectedClassLoader.addPackage("test");
		expectedTestPackage.addClass(loadedClass);
		expectedTestPackage.addPackage("something").addClass(otherClass);
		expectedParentClassLoader.addPackage("test").addClass(rootClass);

		final ClassTreeNode root = classTreeHelper.buildClassLoaderTree(List.of(loadedClass, otherClass, rootClass));

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
		final LoadedClass loadedClass = new LoadedClass("test.TestClass", classLoaderDescriptor, null);
		final LoadedClass otherClass = new LoadedClass("test.something.OtherClass", classLoaderDescriptor, null);
		final LoadedClass rootClass = new LoadedClass("test.Root", parentClassLoaderDescriptor, null);

		final ClassTreeNode expectedRoot = ClassTreeNode.root();
		final ClassTreeNode expectedTestPackage = expectedRoot.addPackage("test");
		expectedTestPackage.addClass(loadedClass);
		expectedTestPackage.addPackage("something").addClass(otherClass);
		expectedTestPackage.addClass(rootClass);

		final ClassTreeNode root = classTreeHelper.buildClassTree(List.of(loadedClass, otherClass, rootClass));

		Assertions.assertEquals(expectedRoot,
		                        root,
		                        expectedRoot.toDetailedString() + "\n!=\n" + root.toDetailedString() + "\n");
	}

	@Test
	void testClassLoaderScope() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

		final FilterableTreeItem<ClassTreeNode> root = buildClassTree();

		final TreeItem<ClassTreeNode> classLoaderNode = root.streamSourceItems()
		                                                    .filter(p -> p.getValue().getType()
		                                                                 == ClassTreeNode.Type.CLASSLOADER)
		                                                    .filter(p -> "SomeClassLoader".equals(p.getValue()
		                                                                                           .getClassLoaderDescriptor()
		                                                                                           .getId()))
		                                                    .findFirst()
		                                                    .orElseThrow();

		final List<LoadedClass> loadedClasses = classTreeHelper.getLoadedClassScope(root, classLoaderNode);

		final LoadedClass someClass = root.streamSource()
		                                  .filter(i -> i.getType() == ClassTreeNode.Type.CLASS)
		                                  .filter(i -> i.getLoadedClass().getName().equals("test.ing.stuff.TestClass"))
		                                  .findFirst()
		                                  .map(ClassTreeNode::getLoadedClass)
		                                  .orElseThrow();

		final LoadedClass someOtherClass = root.streamSource()
		                                       .filter(i -> i.getType() == ClassTreeNode.Type.CLASS)
		                                       .filter(i -> i.getLoadedClass()
		                                                     .getName()
		                                                     .equals("org.test.OtherTestClass"))
		                                       .findFirst()
		                                       .map(ClassTreeNode::getLoadedClass)
		                                       .orElseThrow();

		Assertions.assertEquals(2, loadedClasses.size());
		Assertions.assertTrue(loadedClasses.contains(someClass));
		Assertions.assertFalse(loadedClasses.contains(someOtherClass));
	}

	private FilterableTreeItem<ClassTreeNode> buildClassTree() {
		final ClassTreeNode root = ClassTreeNode.root();
		final ClassLoaderDescriptor someClassLoaderParent = ClassLoaderDescriptor.builder()
		                                                                         .id("SomeClassLoaderParent")
		                                                                         .description(
				                                                                         "SomeClassLoaderParentDescription")
		                                                                         .simpleClassName(
				                                                                         "SomeClassLoaderParent")
		                                                                         .build();
		final ClassLoaderDescriptor someClassLoader = ClassLoaderDescriptor.builder()
		                                                                   .id("SomeClassLoader")
		                                                                   .description("SomeClassLoaderDescription")
		                                                                   .simpleClassName("SomeClassLoader")
		                                                                   .parent(someClassLoaderParent)
		                                                                   .build();
		final ClassLoaderDescriptor someOtherClassLoader = ClassLoaderDescriptor.builder()
		                                                                        .id("SomeOtherClassLoader")
		                                                                        .description(
				                                                                        "SomeOtherClassLoaderDescription")
		                                                                        .simpleClassName(
																						"SomeOtherClassLoader")
		                                                                        .build();
		final LoadedClass someOtherClass = new LoadedClass("org.test.OtherTestClass", someOtherClassLoader, null);
		root.addClassLoader(someOtherClassLoader).addPackage("org").addPackage("test").addClass(someOtherClass);
		final ClassTreeNode parentClassLoader = root.addClassLoader(someClassLoaderParent);
		parentClassLoader.addPackage("org")
		                 .addPackage("test")
		                 .addClass(new LoadedClass("org.test.Test", someClassLoaderParent, null));
		final LoadedClass testClass = new LoadedClass("test.ing.stuff.TestClass", someClassLoader, null);
		parentClassLoader.addClassLoader(someClassLoader)
		                 .addPackage("test")
		                 .addPackage("ing")
		                 .addPackage("stuff")
		                 .addClass(testClass);
		return root.toTreeItem();
	}

	@Test
	void testPackageName() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

		final FilterableTreeItem<ClassTreeNode> root = buildClassTree();

		final TreeItem<ClassTreeNode> someClass = root.streamSourceItems()
		                                              .filter(i -> i.getValue().getType() == ClassTreeNode.Type.CLASS)
		                                              .filter(i -> i.getValue()
		                                                            .getLoadedClass()
		                                                            .getName()
		                                                            .equals("test.ing.stuff.TestClass"))
		                                              .findFirst()
		                                              .map(TreeItem::getParent)
		                                              .orElseThrow();

		final String packageName = classTreeHelper.getPackageName(someClass);

		Assertions.assertEquals("test.ing.stuff", packageName);
	}

	@Test
	void testGetClassesInPackage() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

		final FilterableTreeItem<ClassTreeNode> root = buildClassTree();

		final List<LoadedClass> classesInPackage = classTreeHelper.getClassesInPackage(root, "org", null);

		Assertions.assertEquals(2, classesInPackage.size());
		Assertions.assertTrue(classesInPackage.stream().allMatch(c -> c.getName().startsWith("org")));
	}

	@Test
	void testGetClassesInPackageWithClassLoader() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

		final FilterableTreeItem<ClassTreeNode> root = buildClassTree();

		final ClassLoaderDescriptor classLoader = root.streamSource()
		                                              .filter(i -> i.getType() == ClassTreeNode.Type.CLASSLOADER)
		                                              .filter(i -> i.getClassLoaderDescriptor()
		                                                            .getId()
		                                                            .equals("SomeOtherClassLoader"))
		                                              .findFirst()
		                                              .map(ClassTreeNode::getClassLoaderDescriptor)
		                                              .orElseThrow();

		final List<LoadedClass> classesInPackage = classTreeHelper.getClassesInPackage(root, "org", classLoader);

		Assertions.assertEquals(1, classesInPackage.size());
		Assertions.assertTrue(classesInPackage.stream().allMatch(c -> c.getName().startsWith("org")));
	}

	@Test
	void testGetNodeClassLoader() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

		final FilterableTreeItem<ClassTreeNode> root = buildClassTree();

		final TreeItem<ClassTreeNode> someClass = root.streamSourceItems()
		                                              .filter(i -> i.getValue().getType() == ClassTreeNode.Type.CLASS)
		                                              .filter(i -> i.getValue()
		                                                            .getLoadedClass()
		                                                            .getName()
		                                                            .equals("test.ing.stuff.TestClass"))
		                                              .findFirst()
		                                              .orElseThrow();

		final ClassLoaderDescriptor classLoader = classTreeHelper.getNodeClassLoader(someClass.getParent());

		Assertions.assertNotNull(classLoader);
		final ClassLoaderDescriptor someClassLoader = someClass.getValue().getLoadedClass().getClassLoaderDescriptor();
		Assertions.assertEquals(someClassLoader, classLoader);
	}

	@Test
	void testGetNodeClassLoaderTreeItemStream() {
		final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

		final FilterableTreeItem<ClassTreeNode> root = buildClassTree();

		final TreeItem<ClassTreeNode> someClass = root.streamSourceItems()
		                                              .filter(i -> i.getValue().getType() == ClassTreeNode.Type.CLASS)
		                                              .filter(i -> i.getValue()
		                                                            .getLoadedClass()
		                                                            .getName()
		                                                            .equals("test.ing.stuff.TestClass"))
		                                              .findFirst()
		                                              .orElseThrow();

		final long count = classTreeHelper.getNodeClassLoaderTreeItemStream(someClass).count();

		Assertions.assertEquals(2, count);
	}

}