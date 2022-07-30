package com.github.naton1.jvmexplorer.fx.openclass;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.EditorHelper;
import com.github.naton1.jvmexplorer.helper.FieldTreeHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.WrappedObject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class ClassFieldRowFactory implements Callback<TreeTableView<ClassField>, TreeTableRow<ClassField>> {

	private final FieldTreeHelper fieldTreeHelper = new FieldTreeHelper();

	private final EditorHelper editorHelper;
	private final ExecutorService executorService;
	private final ClientHandler clientHandler;
	private final ObjectProperty<RunningJvm> currentJvm;
	private final AlertHelper alertHelper;
	private final ObjectProperty<ClassContent> currentClass;

	@Override
	public TreeTableRow<ClassField> call(TreeTableView<ClassField> param) {
		final TreeTableRow<ClassField> row = new TreeTableRow<>();
		final ContextMenu rowContextMenu = new ContextMenu();
		final MenuItem editRow = new MenuItem("Edit Value");
		editRow.setOnAction(e -> {
			final ClassField classField = row.getItem();
			if (classField == null) {
				return;
			}
			final String currentValue = editorHelper.getObjectString(classField.getClassFieldKey().getTypeName(),
			                                                         classField.getValue());
			final TextInputDialog dialog = new TextInputDialog(currentValue);
			dialog.setTitle("Update Field");
			dialog.setHeaderText("Enter new value:");
			dialog.setContentText(null);
			dialog.showAndWait().ifPresent(result -> {
				final RunningJvm selectedJvm = currentJvm.get();
				if (selectedJvm == null) {
					return;
				}
				edit(selectedJvm, row.getTreeItem(), result);
			});
		});
		rowContextMenu.getItems().add(editRow);
		// Don't show if the row is empty, or the row is a wrapped object. We can't edit those.
		row.contextMenuProperty().bind(Bindings.when(Bindings.createBooleanBinding(() -> {
			final ClassField item = row.getItem();
			return item != null && !(item.getValue() instanceof WrappedObject);
		}, row.itemProperty())).then(rowContextMenu).otherwise((ContextMenu) null));
		return row;
	}

	private void edit(RunningJvm selectedJvm, TreeItem<ClassField> classField, String newValue) {
		final ClassFieldKey[] classFieldKeys = fieldTreeHelper.getClassFieldKeyPath(classField);
		final Object resultObject = editorHelper.edit(classField.getValue().getClassFieldKey().getTypeName(),
		                                              newValue);
		final ClassLoaderDescriptor currentClassLoader =
				currentClass.get().getLoadedClass().getClassLoaderDescriptor();
		final ClassFieldPath classFieldPath = new ClassFieldPath(classFieldKeys, currentClassLoader);
		executorService.submit(() -> {
			if (clientHandler.setField(selectedJvm, classFieldPath, resultObject)) {
				final ClassField updatedClassField = classField.getValue().withValue(resultObject);
				Platform.runLater(() -> classField.setValue(updatedClassField));
			}
			else {
				Platform.runLater(() -> alertHelper.showError("Operation Failed", "Failed to change field"));
			}
		});
	}

}
