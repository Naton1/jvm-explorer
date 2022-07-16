package com.github.naton1.jvmexplorer.fx.openclass;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;

public class TooltippedTreeTableCell<S, T> extends TreeTableCell<S, T> {

	public TooltippedTreeTableCell() {
		textProperty().bind(Bindings.when(itemProperty().isNotNull()).then(itemProperty().asString()).otherwise(""));
		final Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(itemProperty().asString());
		tooltipProperty().bind(Bindings.when(itemProperty().isNotNull()).then(tooltip).otherwise((Tooltip) null));
	}

}
