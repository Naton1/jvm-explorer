package com.github.naton1.jvmexplorer.fx.classes;

import javafx.beans.binding.Bindings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FilterableTreeItemTest {

	@Test
	void testFilter() {
		final FilterableTreeItem<String> root = buildTree();

		root.predicateProperty().bind(Bindings.createObjectBinding(() -> t -> t.startsWith("t")));

		final long sourceItems = root.getSourceChildren().size();
		final long visibleItems = root.getChildren().size();

		Assertions.assertEquals(1, visibleItems);
		Assertions.assertNotEquals(sourceItems, visibleItems);
	}

	@Test
	void testStreamVisible() {
		final FilterableTreeItem<String> root = buildTree();

		root.predicateProperty().bind(Bindings.createObjectBinding(() -> t -> t.startsWith("t")));

		final long visibleItems = root.streamVisibleItems().count();

		Assertions.assertEquals(1, visibleItems);
	}

	@Test
	void testStreamSource() {
		final FilterableTreeItem<String> root = buildTree();

		final long sourceItems = root.streamSource().count();

		Assertions.assertEquals(4, sourceItems);
	}

	private FilterableTreeItem<String> buildTree() {
		final FilterableTreeItem<String> root = new FilterableTreeItem<>();
		final FilterableTreeItem<String> child = new FilterableTreeItem<>("hmm");
		child.getSourceChildren().addAll(new FilterableTreeItem<>("value"));
		root.getSourceChildren().addAll(child, new FilterableTreeItem<>("test"), new FilterableTreeItem<>("asdf"));
		return root;
	}

}