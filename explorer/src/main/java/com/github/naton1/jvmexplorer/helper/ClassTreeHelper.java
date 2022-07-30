package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.fx.classes.ClassTreeNode;
import com.github.naton1.jvmexplorer.fx.classes.FilterableTreeItem;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ClassTreeHelper {

	public ClassTreeNode buildClassLoaderTree(List<LoadedClass> loadedClasses) {
		final ClassTreeNode classTreeRoot = ClassTreeNode.root();
		for (LoadedClass loadedClass : loadedClasses) {
			ClassTreeNode classRoot = classTreeRoot;
			if (loadedClass.getClassLoaderDescriptor() != null) {
				classRoot = addClassLoader(loadedClass, classTreeRoot);
			}
			addClass(loadedClass, classRoot);
		}
		return classTreeRoot;
	}

	public ClassTreeNode buildClassTree(List<LoadedClass> loadedClasses) {
		final ClassTreeNode classTreeRoot = ClassTreeNode.root();
		for (LoadedClass loadedClass : loadedClasses) {
			addClass(loadedClass, classTreeRoot);
		}
		return classTreeRoot;
	}

	private ClassTreeNode addClassLoader(LoadedClass loadedClass, ClassTreeNode treeRoot) {
		ClassTreeNode classLoaderTree = treeRoot;
		final List<ClassLoaderDescriptor> classLoaders = Stream.iterate(loadedClass.getClassLoaderDescriptor(),
		                                                                Objects::nonNull,
		                                                                ClassLoaderDescriptor::getParent)
		                                                       .collect(Collectors.toCollection(ArrayList::new));
		Collections.reverse(classLoaders);

		for (ClassLoaderDescriptor classLoaderDescriptor : classLoaders) {
			classLoaderTree = classLoaderTree.addClassLoader(classLoaderDescriptor);
		}

		return classLoaderTree;
	}

	private void addClass(LoadedClass loadedClass, ClassTreeNode classRoot) {
		final String[] classNameParts = loadedClass.getName().split("\\.");
		ClassTreeNode classTree = classRoot;
		for (int i = 0; i < classNameParts.length - 1; i++) {
			final String classNamePart = classNameParts[i];
			classTree = classTree.addPackage(classNamePart);
		}
		classTree.addClass(loadedClass);
	}

	public List<LoadedClass> getLoadedClassScope(FilterableTreeItem<ClassTreeNode> classesTreeRoot,
	                                             TreeItem<ClassTreeNode> classLoaderNode) {
		if (classLoaderNode == null) {
			return classesTreeRoot.streamSource()
			                      .filter(p -> p.getType() == ClassTreeNode.Type.CLASS)
			                      .map(ClassTreeNode::getLoadedClass)
			                      .collect(Collectors.toList());
		}
		return getNodeClassLoaderTreeItemStream(classLoaderNode).map(this::getClassesLoadedIn)
		                                                        .flatMap(List::stream)
		                                                        .collect(Collectors.toList());
	}

	private List<LoadedClass> getClassesLoadedIn(TreeItem<ClassTreeNode> classLoaderNode) {
		final Queue<TreeItem<ClassTreeNode>> frontier = new ArrayDeque<>(getChildren(classLoaderNode));
		final List<LoadedClass> loadedClasses = new ArrayList<>();
		while (!frontier.isEmpty()) {
			final TreeItem<ClassTreeNode> next = frontier.poll();
			final ClassTreeNode nextNode = next.getValue();
			if (nextNode.getType() == ClassTreeNode.Type.CLASS) {
				loadedClasses.add(nextNode.getLoadedClass());
			}
			if (nextNode.getType() == ClassTreeNode.Type.PACKAGE) {
				frontier.addAll(getChildren(next));
			}
		}
		return loadedClasses;
	}

	private List<TreeItem<ClassTreeNode>> getChildren(TreeItem<ClassTreeNode> treeItem) {
		if (treeItem instanceof FilterableTreeItem) {
			return ((FilterableTreeItem<ClassTreeNode>) treeItem).getSourceChildren();
		}
		log.warn("Trying to get children of regular tree node {}", treeItem);
		return treeItem.getChildren();
	}

	public String getPackageName(TreeItem<ClassTreeNode> packageNode) {
		final List<String> packageParts = Stream.iterate(packageNode,
		                                                 o -> o != null && o.getValue() != null
		                                                      && o.getValue().getType() == ClassTreeNode.Type.PACKAGE,
		                                                 TreeItem::getParent)
		                                        .map(item -> item.getValue().getPackageSegment())
		                                        .collect(Collectors.toCollection(ArrayList::new));
		Collections.reverse(packageParts);
		return String.join(".", packageParts);
	}

	// Note this includes all subpackages as well
	public List<LoadedClass> getClassesInPackage(FilterableTreeItem<ClassTreeNode> classesTreeRoot,
	                                             String fullPackageName, ClassLoaderDescriptor packageClassLoader) {
		return classesTreeRoot.streamVisible()
		                      .filter(p -> p.getType() == ClassTreeNode.Type.CLASS)
		                      .map(ClassTreeNode::getLoadedClass)
		                      .filter(c -> c.getName().startsWith(fullPackageName))
		                      .filter(c -> (packageClassLoader == null)
		                                   || packageClassLoader.equals(c.getClassLoaderDescriptor()))
		                      .collect(Collectors.toList());
	}

	public Stream<TreeItem<ClassTreeNode>> getNodeClassLoaderTreeItemStream(TreeItem<ClassTreeNode> treeItem) {
		return Stream.iterate(treeItem, o -> o != null && o.getValue() != null, TreeItem::getParent)
		             .filter(p -> p.getValue().getType() == ClassTreeNode.Type.CLASSLOADER);
	}

	public TreeItem<ClassTreeNode> getNodeClassLoaderTreeItem(TreeItem<ClassTreeNode> treeItem) {
		return getNodeClassLoaderTreeItemStream(treeItem).findFirst().orElse(null);
	}

	public ClassTreeNode getNodeClassLoaderNode(TreeItem<ClassTreeNode> treeItem) {
		final TreeItem<ClassTreeNode> classTreeNode = getNodeClassLoaderTreeItem(treeItem);
		return classTreeNode != null ? classTreeNode.getValue() : null;
	}

	public ClassLoaderDescriptor getNodeClassLoader(TreeItem<ClassTreeNode> treeItem) {
		final ClassTreeNode classTreeNode = getNodeClassLoaderNode(treeItem);
		return classTreeNode != null ? classTreeNode.getClassLoaderDescriptor() : null;
	}

}
