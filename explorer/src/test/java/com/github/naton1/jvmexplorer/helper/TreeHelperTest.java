package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TreeHelperTest {

	@Test
	void testGetClassFieldKeys() {
		final TreeHelper treeHelper = new TreeHelper();

		final TreeItem<ClassField> root = new TreeItem<>();
		root.setValue(new ClassField(null, "a"));

		final TreeItem<ClassField> randomChild = new TreeItem<>();
		final ClassFieldKey randomChildClassFieldKey = new ClassFieldKey("class", "field", "type", 0);
		randomChild.setValue(new ClassField(randomChildClassFieldKey, "b"));

		final TreeItem<ClassField> parent = new TreeItem<>();
		final ClassFieldKey parentClassFieldKey = new ClassFieldKey("class", "field", "type", 0);
		parent.setValue(new ClassField(parentClassFieldKey, "cb"));

		final TreeItem<ClassField> selectedField = new TreeItem<>();
		final ClassFieldKey selectedClassFieldKey = new ClassFieldKey("class", "field", "type", 0);
		selectedField.setValue(new ClassField(selectedClassFieldKey, "d"));

		final TreeItem<ClassField> child = new TreeItem<>();
		final ClassFieldKey childClassFieldKey = new ClassFieldKey("class", "field", "type", 0);
		child.setValue(new ClassField(childClassFieldKey, "e"));

		root.getChildren().add(parent);
		root.getChildren().add(randomChild);
		parent.getChildren().add(selectedField);
		selectedField.getChildren().add(child);

		final ClassFieldKey[] classFieldKeys = treeHelper.getClassFieldKeyPath(selectedField);

		final ClassFieldKey[] expected = { parentClassFieldKey, selectedClassFieldKey };

		Assertions.assertArrayEquals(expected, classFieldKeys);
	}

}