package com.github.naton1.jvmexplorer.fx.classes;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.TreeViewSkin;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

// Note: we are making an assumption that the tree view root is never null, and the tree view is empty if the root
// has no children
@Slf4j
public class TreeViewPlaceholderSkin<T> extends TreeViewSkin<T> {

	private final SimpleObjectProperty<Node> placeholderProperty;

	private StackPane placeholderRegion;

	public TreeViewPlaceholderSkin(TreeView<T> control) {
		super(control);
		placeholderProperty = new SimpleObjectProperty<>();
		placeholderProperty.addListener((obs, old, newv) -> {
			if (placeholderRegion != null) {
				placeholderRegion.getChildren().setAll(newv);
			}
		});
		registerChangeListener(Bindings.isEmpty(getSkinnable().getRoot().getChildren()),
		                       e -> updatePlaceholderSupport());
		updatePlaceholderSupport();
	}

	private void updatePlaceholderSupport() {
		final boolean empty = isTreeEmpty();
		if (empty) {
			if (placeholderRegion == null) {
				placeholderRegion = new StackPane();
				placeholderRegion.getStyleClass().setAll("placeholder");
				getChildren().add(placeholderRegion);
				final Node placeholder = placeholderProperty.get();
				if (placeholder != null) {
					placeholderRegion.getChildren().setAll(placeholder);
				}
			}
		}
		getVirtualFlow().setVisible(!empty);
		if (placeholderRegion != null) {
			placeholderRegion.setVisible(empty);
		}
	}

	private boolean isTreeEmpty() {
		return getSkinnable().getRoot().getChildren().isEmpty();
	}

	public Property<Node> placeholderProperty() {
		return placeholderProperty;
	}

	@Override
	protected void layoutChildren(double x, double y, double w, double h) {
		super.layoutChildren(x, y, w, h);
		if (placeholderRegion != null && placeholderRegion.isVisible()) {
			placeholderRegion.resizeRelocate(x, y, w, h);
		}
	}

}
