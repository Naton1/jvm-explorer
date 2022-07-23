package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.fx.classes.PackageTreeNode;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ClassTreeHelper {

	public PackageTreeNode buildClassLoaderTree(List<LoadedClass> loadedClasses) {
		final PackageTreeNode packageTreeRoot = PackageTreeNode.root();
		for (LoadedClass loadedClass : loadedClasses) {
			PackageTreeNode classRoot = packageTreeRoot;
			if (loadedClass.getClassLoaderDescriptor() != null) {
				classRoot = addClassLoader(loadedClass, packageTreeRoot);
			}
			addClass(loadedClass, classRoot);
		}
		return packageTreeRoot;
	}

	public PackageTreeNode buildClassTree(List<LoadedClass> loadedClasses) {
		final PackageTreeNode packageTreeRoot = PackageTreeNode.root();
		for (LoadedClass loadedClass : loadedClasses) {
			addClass(loadedClass, packageTreeRoot);
		}
		return packageTreeRoot;
	}

	private PackageTreeNode addClassLoader(LoadedClass loadedClass, PackageTreeNode treeRoot) {
		PackageTreeNode classLoaderTree = treeRoot;
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

	private void addClass(LoadedClass loadedClass, PackageTreeNode classRoot) {
		final String[] classNameParts = loadedClass.getName().split("\\.");
		PackageTreeNode packageTree = classRoot;
		for (int i = 0; i < classNameParts.length - 1; i++) {
			final String classNamePart = classNameParts[i];
			packageTree = packageTree.addPackage(classNamePart);
		}
		packageTree.addClass(loadedClass);
	}

}
