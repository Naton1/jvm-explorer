package com.github.naton1.jvmexplorer.fx.openclass;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ClipboardHelper;
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
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class ClassFieldCellFactory implements Callback<TreeView<ClassField>, TreeCell<ClassField>> {

	private final FieldTreeHelper fieldTreeHelper = new FieldTreeHelper();

	private final EditorHelper editorHelper;
	private final ExecutorService executorService;
	private final ClientHandler clientHandler;
	private final ObjectProperty<RunningJvm> currentJvm;
	private final AlertHelper alertHelper;
	private final ObjectProperty<ClassContent> currentClass;

	@Override
	public TreeCell<ClassField> call(TreeView<ClassField> param) {
		final TreeCell<ClassField> cell = new TreeCell<>();
		setupContextMenu(cell);
		setupTextBinding(cell);
		setupImageBinding(cell);
		setupTooltipBinding(cell);
		return cell;
	}

	private void setupContextMenu(TreeCell<ClassField> cell) {
		final ContextMenu rowContextMenu = new ContextMenu();
		final MenuItem editRow = new MenuItem("Edit Value");
		editRow.setOnAction(e -> {
			final ClassField classField = cell.getItem();
			if (classField == null) {
				return;
			}
			final String currentValue = editorHelper.getObjectString(classField.getClassFieldKey().getTypeName(),
			                                                         classField.getValue());
			final TextInputDialog dialog = new TextInputDialog(currentValue);
			dialog.setTitle("Update Field");
			dialog.setHeaderText("Enter new value:");
			dialog.setContentText(null);
			dialog.initOwner(cell.getScene().getWindow());
			dialog.showAndWait().ifPresent(result -> {
				final RunningJvm selectedJvm = currentJvm.get();
				if (selectedJvm == null) {
					return;
				}
				edit(selectedJvm, cell.getTreeItem(), result);
			});
		});
		final MenuItem copyValue = new MenuItem("Copy Value");
		copyValue.setOnAction(e -> {
			final ClassField classField = cell.getItem();
			if (classField == null) {
				return;
			}
			final String stringValue = String.valueOf(classField.getValue());
			ClipboardHelper.copy(stringValue);
		});
		cell.itemProperty().addListener((obs, old, newv) -> {
			rowContextMenu.getItems().clear();
			if (newv != null) {
				if (!(newv.getValue() instanceof WrappedObject)) {
					rowContextMenu.getItems().addAll(editRow);
				}
				rowContextMenu.getItems().addAll(copyValue);
			}
		});
		cell.setContextMenu(rowContextMenu);
	}

	private void setupTextBinding(TreeCell<ClassField> treeCell) {
		treeCell.textProperty()
		        .bind(Bindings.when(treeCell.itemProperty().isNotNull())
		                      .then(treeCell.itemProperty().asString())
		                      .otherwise(""));
	}

	private void setupImageBinding(TreeCell<ClassField> treeCell) {
		treeCell.graphicProperty().bind(Bindings.createObjectBinding(() -> {
			final ClassField item = treeCell.getItem();
			if (item == null) {
				return null;
			}
			return new ImageView(getFieldType(item).getImage());
		}, treeCell.itemProperty()));
	}

	private void setupTooltipBinding(TreeCell<ClassField> treeCell) {
		final Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(treeCell.itemProperty().asString());
		treeCell.tooltipProperty()
		        .bind(Bindings.when(treeCell.itemProperty().isNotNull()).then(tooltip).otherwise((Tooltip) null));
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

	private FieldType getFieldType(ClassField classField) {
		if (Modifier.isStatic(classField.getClassFieldKey().getModifiers())) {
			if (Modifier.isFinal(classField.getClassFieldKey().getModifiers())) {
				return FieldType.CONSTANT;
			}
			return FieldType.STATIC;
		}
		else {
			return FieldType.INSTANCE;
		}
	}

	private enum FieldType {
		STATIC("icons/static.png"), INSTANCE("icons/field.png"), CONSTANT("icons/constant.png");
		@Getter
		private final Image image;

		FieldType(String imagePath) {
			image = new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(imagePath)));
		}
	}

}
