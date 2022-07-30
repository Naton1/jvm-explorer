package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldTreeHelper {

	public ClassFieldKey[] getClassFieldKeyPath(TreeItem<ClassField> treeItemChild) {
		final List<ClassFieldKey> classFieldKeys = new ArrayList<>();
		TreeItem<ClassField> currentTreeItem = treeItemChild;
		// Check parent not null too since we don't want the root
		while (currentTreeItem != null && currentTreeItem.getParent() != null) {
			classFieldKeys.add(currentTreeItem.getValue().getClassFieldKey());
			currentTreeItem = currentTreeItem.getParent();
		}
		Collections.reverse(classFieldKeys);
		return classFieldKeys.toArray(ClassFieldKey[]::new);
	}

}
