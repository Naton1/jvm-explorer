package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Window;

public class DialogHelper {

	public static void initCustomDialog(Dialog<?> dialog, ObjectProperty<RunningJvm> currentJvm) {
		dialog.initModality(Modality.NONE);
		dialog.setResizable(true);
		dialog.getDialogPane().getButtonTypes().setAll();
		final Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
		final ChangeListener<RunningJvm> changeListener = (obs, old, newv) -> dialogWindow.hide();
		dialog.setOnHidden(e -> currentJvm.removeListener(changeListener));
		currentJvm.addListener(changeListener);
		dialogWindow.setOnCloseRequest(e -> dialog.hide());
		dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				dialogWindow.hide();
			}
		});
		dialog.getDialogPane().getStyleClass().add("custom-dialog-pane");
	}

}
