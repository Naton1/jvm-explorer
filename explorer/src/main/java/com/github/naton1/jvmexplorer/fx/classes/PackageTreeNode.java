package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.Value;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Value
public class PackageTreeNode implements Comparable<PackageTreeNode> {

	private final Map<String, PackageTreeNode> children = new HashMap<>();
	private final LoadedClass loadedClass;
	private final String packagePart;

	public FilterableTreeItem<PackageTreeNode> toTreeItem() {
		final FilterableTreeItem<PackageTreeNode> treeItem = new FilterableTreeItem<>(this);
		children.forEach((key, value) -> treeItem.getSourceChildren().add(value.toTreeItem()));
		treeItem.getSourceChildren().sort(Comparator.comparing(TreeItem::getValue));
		return treeItem;
	}

	@Override
	public String toString() {
		return packagePart;
	}

	@Override
	public int compareTo(PackageTreeNode o) {
		if (this.getLoadedClass() == null && o.getLoadedClass() == null) {
			return getPackagePart().compareTo(o.getPackagePart());
		}
		else if (this.getLoadedClass() != null && o.getLoadedClass() != null) {
			return this.getLoadedClass().compareTo(o.getLoadedClass());
		}
		else {
			return this.getLoadedClass() == null ? -1 : 1;
		}
	}

	public Type getType() {
		if (loadedClass != null) {
			return Type.CLASS;
		}
		return Type.PACKAGE;
	}

	public enum Type {
		PACKAGE("icons/package.png"), CLASS("icons/class.png");
		@Getter
		private final Image image;

		Type(String imagePath) {
			image = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imagePath)));
		}
	}

}
