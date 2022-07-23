package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
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
public class PackageTreeNode implements Comparable<PackageTreeNode> {

	private final Map<String, PackageTreeNode> children = new HashMap<>();
	private final LoadedClass loadedClass;
	private final String packagePart;
	private final ClassLoaderDescriptor classLoaderDescriptor;

	public FilterableTreeItem<PackageTreeNode> toTreeItem() {
		final FilterableTreeItem<PackageTreeNode> treeItem = new FilterableTreeItem<>(this);
		children.forEach((key, value) -> treeItem.getSourceChildren().add(value.toTreeItem()));
		treeItem.getSourceChildren().sort(Comparator.comparing(TreeItem::getValue));
		return treeItem;
	}

	public PackageTreeNode addPackage(String name) {
		final String key = getKeyForPackage(name);
		return children.computeIfAbsent(key, k -> PackageTreeNode.ofPackage(name));
	}

	public PackageTreeNode addClass(LoadedClass loadedClass) {
		final String key = getKeyForClass(loadedClass);
		final PackageTreeNode classNode = PackageTreeNode.ofClass(loadedClass);
		final PackageTreeNode previous = children.put(key, classNode);
		if (previous != null) {
			log.warn("Loaded duplicate class: {}", loadedClass);
		}
		return classNode;
	}

	public PackageTreeNode addClassLoader(ClassLoaderDescriptor classLoaderDescriptor) {
		final String key = getKeyForClassLoader(classLoaderDescriptor);
		return children.computeIfAbsent(key, k -> PackageTreeNode.ofClassLoader(classLoaderDescriptor));
	}

	@Override
	public String toString() {
		return packagePart;
	}

	@Override
	public int compareTo(PackageTreeNode o) {
		// ClassLoader > Package > Class, then compare displayName
		return Comparator.<PackageTreeNode>comparingInt(node -> node.getType().ordinal())
		                 .thenComparing(PackageTreeNode::getPackagePart)
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

	public static PackageTreeNode root() {
		return new PackageTreeNode(null, null, null);
	}

	private static PackageTreeNode ofClass(LoadedClass loadedClass) {
		return new PackageTreeNode(loadedClass, loadedClass.getSimpleName(), null);
	}

	private static PackageTreeNode ofPackage(String packagePart) {
		return new PackageTreeNode(null, packagePart, null);
	}

	private static PackageTreeNode ofClassLoader(ClassLoaderDescriptor classLoaderDescriptor) {
		return new PackageTreeNode(null, classLoaderDescriptor.getDescription(), classLoaderDescriptor);
	}

	private String getKeyForClass(LoadedClass loadedClass) {
		return PackageTreeNode.Type.CLASS.name() + ":" + loadedClass.getSimpleName();
	}

	private String getKeyForPackage(String packagePart) {
		return PackageTreeNode.Type.PACKAGE.name() + ":" + packagePart;
	}

	private String getKeyForClassLoader(ClassLoaderDescriptor classLoaderDescriptor) {
		return PackageTreeNode.Type.CLASSLOADER.name() + ":" + classLoaderDescriptor.getId();
	}

}
