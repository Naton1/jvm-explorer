package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.protocol.ActiveClass;
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
	private final ActiveClass activeClass;
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
		if (getActiveClass() == null && o.getActiveClass() == null) {
			return getPackagePart().compareTo(o.getPackagePart());
		}
		else if (getActiveClass() != null && o.getActiveClass() != null) {
			return getActiveClass().compareTo(o.getActiveClass());
		}
		else {
			return getActiveClass() == null ? -1 : 1;
		}
	}

	public Type getType() {
		if (activeClass != null) {
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
