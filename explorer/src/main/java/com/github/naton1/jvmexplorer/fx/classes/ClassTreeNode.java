package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ClassTreeNode implements Comparable<ClassTreeNode> {

	@Getter(AccessLevel.PRIVATE)
	private final Map<String, ClassTreeNode> children = new HashMap<>();
	private final LoadedClass loadedClass;
	private final String packageSegment;
	private final ClassLoaderDescriptor classLoaderDescriptor;

	public FilterableTreeItem<ClassTreeNode> toTreeItem() {
		final FilterableTreeItem<ClassTreeNode> treeItem = new FilterableTreeItem<>(this);
		children.forEach((key, value) -> treeItem.getSourceChildren().add(value.toTreeItem()));
		treeItem.getSourceChildren().sort(Comparator.comparing(TreeItem::getValue));
		return treeItem;
	}

	public ClassTreeNode addPackage(String name) {
		final String key = getKeyForPackage(name);
		return children.computeIfAbsent(key, k -> ClassTreeNode.ofPackage(name));
	}

	public ClassTreeNode addClass(LoadedClass loadedClass) {
		final String key = getKeyForClass(loadedClass);
		final ClassTreeNode classNode = ClassTreeNode.ofClass(loadedClass);
		final ClassTreeNode previous = children.put(key, classNode);
		if (previous != null) {
			log.warn("Loaded duplicate class: {}", loadedClass);
		}
		return classNode;
	}

	public ClassTreeNode addClassLoader(ClassLoaderDescriptor classLoaderDescriptor) {
		final String key = getKeyForClassLoader(classLoaderDescriptor);
		return children.computeIfAbsent(key, k -> ClassTreeNode.ofClassLoader(classLoaderDescriptor));
	}

	// Mainly for debugging purposes
	public String toDetailedString() {
		return toDetailedString(0);
	}

	private String toDetailedString(int indent) {
		final String nodeString = getType() + "-" + this;
		final String childrenString = getChildren().entrySet()
		                                           .stream()
		                                           .map(e -> e.getKey() + "-" + e.getValue()
		                                                                         .toDetailedString(indent + 1))
		                                           .collect(Collectors.joining(",\n" + "\t".repeat(indent + 1)));
		return nodeString + " [" + childrenString + "]";
	}

	@Override
	public String toString() {
		return packageSegment;
	}

	@Override
	public int compareTo(ClassTreeNode o) {
		// ClassLoader > Package > Class, then compare displayName
		return Comparator.<ClassTreeNode>comparingInt(node -> node.getType().ordinal())
		                 .thenComparing(ClassTreeNode::getPackageSegment)
		                 .compare(this, o);
	}

	public Type getType() {
		if (loadedClass != null) {
			return Type.CLASS;
		}
		else if (classLoaderDescriptor != null) {
			return Type.CLASSLOADER;
		}
		else {
			return Type.PACKAGE;
		}
	}

	public enum Type {
		CLASSLOADER("icons/classloader.png"), PACKAGE("icons/package.png"), CLASS("icons/class.png"),
		;
		@Getter
		private final Image image;

		Type(String imagePath) {
			image = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imagePath)));
		}
	}

	public static ClassTreeNode root() {
		return new ClassTreeNode(null, null, null);
	}

	private static ClassTreeNode ofClass(LoadedClass loadedClass) {
		return new ClassTreeNode(loadedClass, loadedClass.getSimpleName(), null);
	}

	private static ClassTreeNode ofPackage(String packagePart) {
		return new ClassTreeNode(null, packagePart, null);
	}

	private static ClassTreeNode ofClassLoader(ClassLoaderDescriptor classLoaderDescriptor) {
		return new ClassTreeNode(null, classLoaderDescriptor.getSimpleClassName(), classLoaderDescriptor);
	}

	private String getKeyForClass(LoadedClass loadedClass) {
		return ClassTreeNode.Type.CLASS.name() + ":" + loadedClass.getSimpleName();
	}

	private String getKeyForPackage(String packagePart) {
		return ClassTreeNode.Type.PACKAGE.name() + ":" + packagePart;
	}

	private String getKeyForClassLoader(ClassLoaderDescriptor classLoaderDescriptor) {
		return ClassTreeNode.Type.CLASSLOADER.name() + ":" + classLoaderDescriptor.getId();
	}

}
