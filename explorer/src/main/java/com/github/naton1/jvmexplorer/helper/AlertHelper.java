package com.github.naton1.jvmexplorer.helper;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class AlertHelper {

	private final Stage ownerStage;

	public void showError(String title, String headerText, Exception ex) {
		final Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(headerText);
		alert.setContentText(ex.getMessage());
		final Tooltip tooltip = new Tooltip(ex.getMessage());
		Tooltip.install(alert.getDialogPane(), tooltip);
		alert.initOwner(ownerStage);
		alert.showAndWait();
		Tooltip.uninstall(alert.getDialogPane(), tooltip);
	}

	public void showExpandableList(String title, String headerText, String contentText, List<String> list) {
		final Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(headerText);
		alert.setContentText(contentText);
		final ListView<String> stringListView = new ListView<>();
		stringListView.getItems().setAll(list);
		stringListView.getItems().sort(Comparator.naturalOrder());
		alert.getDialogPane().setExpandableContent(stringListView);
		alert.initOwner(ownerStage);
		alert.showAndWait();
	}

	public void showObservableInfo(ObservableValue<String> title, ObservableValue<String> headerText) {
		final Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Export In Progress");
		alert.titleProperty().bind(title);
		alert.headerTextProperty().bind(headerText);
		alert.setContentText(null);
		alert.initOwner(ownerStage);
		alert.showAndWait();
	}

	public void show(Alert.AlertType alertType, String titleText, String headerText) {
		final Alert alert = new Alert(alertType);
		alert.setTitle(titleText);
		alert.setHeaderText(headerText);
		alert.setContentText(null);
		alert.initOwner(ownerStage);
		alert.showAndWait();
	}

	public void showError(String titleText, String headerText) {
		if (headerText != null && headerText.length() > 500) {
			headerText = headerText.substring(0, 500) + "...";
		}
		final Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(titleText);
		alert.setHeaderText(headerText);
		alert.setContentText(null);
		alert.initOwner(this.ownerStage);
		alert.showAndWait();
	}

}
