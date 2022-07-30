package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClassTreeNodeTest {

	@Test
	void testToTreeItem() {
		final ClassTreeNode root = ClassTreeNode.root();
		root.addPackage("test").addClass(new LoadedClass("test.Test", null));
		root.addClass(new LoadedClass("OtherClass", null));

		final FilterableTreeItem<ClassTreeNode> rootTreeItem = root.toTreeItem();

		Assertions.assertEquals(2, rootTreeItem.getChildren().size());

		final TreeItem<ClassTreeNode> childPackage = rootTreeItem.getChildren().get(0);
		Assertions.assertSame(childPackage.getValue().getType(), ClassTreeNode.Type.PACKAGE);
		Assertions.assertSame(rootTreeItem.getChildren().get(1).getValue().getType(), ClassTreeNode.Type.CLASS);

		Assertions.assertEquals(1, childPackage.getChildren().size());
		Assertions.assertEquals("test.Test", childPackage.getChildren().get(0).getValue().getLoadedClass().getName());
	}

}