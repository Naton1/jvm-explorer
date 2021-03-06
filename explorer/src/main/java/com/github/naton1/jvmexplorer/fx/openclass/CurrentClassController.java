package com.github.naton1.jvmexplorer.fx.openclass;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.bytecode.AsmDisassembler;
import com.github.naton1.jvmexplorer.bytecode.BytecodeTextifier;
import com.github.naton1.jvmexplorer.bytecode.QuiltflowerDecompiler;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.CodeAreaHelper;
import com.github.naton1.jvmexplorer.helper.EditorHelper;
import com.github.naton1.jvmexplorer.helper.FieldTreeHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.WrappedObject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CurrentClassController {

	private static final String NO_CLASS_FILE_OPEN = "Select a loaded class to open";
	private static final String PROCESSOR_FAILED = "Processor failed";

	private final EditorHelper editorHelper = new EditorHelper();
	private final FieldTreeHelper fieldTreeHelper = new FieldTreeHelper();

	@FXML
	private CodeArea bytecode;

	@FXML
	private CodeArea classFile;

	@FXML
	private TreeTableView<ClassField> classFields;

	@FXML
	private TreeTableColumn<ClassField, String> modifiersColumn;

	@FXML
	private TreeTableColumn<ClassField, String> typeColumn;

	@FXML
	private TreeTableColumn<ClassField, String> nameColumn;

	@FXML
	private TreeTableColumn<ClassField, Object> valueColumn;

	@FXML
	private TitledPane loadedClassTitlePane;

	private ObjectProperty<RunningJvm> currentJvm;
	private ObjectProperty<ClassContent> currentClass;

	private ScheduledExecutorService executorService;
	private ClientHandler clientHandler;
	private AlertHelper alertHelper;

	private CodeAreaHelper codeAreaHelper;

	public void initialize(Stage stage, ScheduledExecutorService executorService, ClientHandler clientHandler,
	                       ObjectProperty<RunningJvm> currentJvm, ObjectProperty<ClassContent> currentClass) {
		this.executorService = executorService;
		this.clientHandler = clientHandler;
		this.currentJvm = currentJvm;
		this.currentClass = currentClass;
		this.alertHelper = new AlertHelper(stage);
		this.codeAreaHelper = new CodeAreaHelper(executorService);
		initialize();
	}

	private void initialize() {

		setupTitlePaneText();

		currentClass.addListener((obs, old, newv) -> onClassChange(old, newv));

		setupClassFieldTree();

		setupCodeArea(classFile);
		setupCodeArea(bytecode);
	}

	private void setupTitlePaneText() {
		loadedClassTitlePane.textProperty().bind(Bindings.createStringBinding(() -> {
			final ClassContent currentClassContent = currentClass.get();
			if (currentClassContent != null) {
				return "Class: " + currentClassContent.getLoadedClass();
			}
			return "Class: None";
		}, currentClass));
	}

	private void onClassChange(ClassContent old, ClassContent newv) {
		if (newv == null) {
			classFile.clear();
			bytecode.clear();
			classFields.getRoot().getChildren().clear();
		}
		else {
			processBytecode(newv, new QuiltflowerDecompiler(), classFile);
			processBytecode(newv, new AsmDisassembler(), bytecode);
			loadChildren(classFields.getRoot(), newv.getClassFields());
		}
	}

	private void processBytecode(ClassContent classContent, BytecodeTextifier bytecodeTextifier, CodeArea codeArea) {
		final String currentContent = codeArea.getText();
		final Future<?> processingPlaceholderTask = executorService.schedule(() -> {
			Platform.runLater(() -> {
				// Don't update if the class was switched again
				if (!classContent.equals(currentClass.get())) {
					return;
				}
				final String updatedContent = codeArea.getText();
				// Still not updated
				if (updatedContent.equals(currentContent)) {
					codeArea.clear();
					log.trace("Class file is taking some time to process... showing placeholder");
					// The placeholder will know that if there's no text and a class file, it's processing
				}
			});
		}, 500L, TimeUnit.MILLISECONDS);
		executorService.submit(() -> {
			log.debug("Processing class {} with {}", classContent.getLoadedClass(), bytecodeTextifier);
			final String processedClass = classContent.getClassContent().length > 0 ? bytecodeTextifier.process(
					classContent.getClassContent()) : null;
			processingPlaceholderTask.cancel(false);
			Platform.runLater(() -> {
				// Don't update if the class was switched again
				if (!classContent.equals(currentClass.get())) {
					log.debug("Class updated; not showing output for {} with processor {}",
					          classContent.getLoadedClass(),
					          bytecodeTextifier);
					return;
				}
				if (processedClass == null || processedClass.isEmpty()) {
					log.warn("Process result is empty for class {} with processor {}",
					         classContent.getLoadedClass(),
					         bytecodeTextifier);
					codeArea.replaceText(PROCESSOR_FAILED);
					return;
				}
				codeArea.replaceText(processedClass);
				// sometimes the text area wasn't scrolling to 0 by default, so let's tell it to no matter what
				codeArea.scrollYToPixel(0);
				// Trigger update immediately
				codeAreaHelper.triggerHighlightUpdate(codeArea);
			});
		});
	}

	private void setupClassFieldTree() {
		final TreeItem<ClassField> classFieldRoot = new TreeItem<>(null);
		classFields.setShowRoot(false);
		classFields.setRoot(classFieldRoot);
		classFields.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);

		modifiersColumn.setCellValueFactory(cv -> new SimpleObjectProperty<>(getModifiers(cv)));
		typeColumn.setCellValueFactory(cv -> new SimpleObjectProperty<>(getType(cv)));
		nameColumn.setCellValueFactory(cv -> new SimpleObjectProperty<>(getName(cv)));
		valueColumn.setCellValueFactory(cv -> new SimpleObjectProperty<>(getValue(cv)));

		modifiersColumn.setCellFactory(tv -> new TooltippedTreeTableCell<>());
		typeColumn.setCellFactory(tv -> new TooltippedTreeTableCell<>());
		nameColumn.setCellFactory(tv -> new TooltippedTreeTableCell<>());
		valueColumn.setCellFactory(tv -> new TooltippedTreeTableCell<>());

		classFields.setRowFactory(new ClassFieldRowFactory(editorHelper,
		                                                   executorService,
		                                                   clientHandler,
		                                                   currentJvm,
		                                                   alertHelper,
		                                                   currentClass));

		final Label classFieldsPlaceholder = new Label();
		classFieldsPlaceholder.textProperty()
		                      .bind(Bindings.createStringBinding(() -> currentClass.get() != null
		                                                               ? "No static fields found" : NO_CLASS_FILE_OPEN,
		                                                         currentClass));
		classFields.setPlaceholder(classFieldsPlaceholder);
	}

	private String getModifiers(TreeTableColumn.CellDataFeatures<ClassField, String> cellDataFeatures) {
		final TreeItem<ClassField> treeItem = cellDataFeatures.getValue();
		final ClassField classField = treeItem.getValue();
		if (classField == null) {
			return null;
		}
		final String modifiers = Modifier.toString(classField.getClassFieldKey().getModifiers());
		if (modifiers.isEmpty()) {
			return "<none>";
		}
		return modifiers;
	}

	private String getType(TreeTableColumn.CellDataFeatures<ClassField, String> cellDataFeatures) {
		final TreeItem<ClassField> treeItem = cellDataFeatures.getValue();
		final ClassField classField = treeItem.getValue();
		if (classField == null) {
			return null;
		}
		return classField.getClassFieldKey().getTypeName();
	}

	private String getName(TreeTableColumn.CellDataFeatures<ClassField, String> cellDataFeatures) {
		final TreeItem<ClassField> treeItem = cellDataFeatures.getValue();
		final ClassField classField = treeItem.getValue();
		if (classField == null) {
			return null;
		}
		final ClassFieldKey classFieldKey = classField.getClassFieldKey();
		return classFieldKey.getSimpleName() + "." + classFieldKey.getFieldName();
	}

	private Object getValue(TreeTableColumn.CellDataFeatures<ClassField, Object> cellDataFeatures) {
		final TreeItem<ClassField> treeItem = cellDataFeatures.getValue();
		final ClassField classField = treeItem.getValue();
		if (classField == null) {
			return null;
		}
		return classField.getValue();
	}

	// CodeArea must be in a VBox to replace and insert into VirtualizedScrollPane
	private void setupCodeArea(CodeArea codeArea) {
		codeArea.setEditable(false);
		final Label placeholderLabel = new Label();
		placeholderLabel.textProperty()
		                .bind(Bindings.when(this.currentClass.isNotNull())
		                              .then("Processing class")
		                              .otherwise(NO_CLASS_FILE_OPEN));
		codeArea.setPlaceholder(placeholderLabel);
		codeArea.mouseTransparentProperty()
		        .bind(Bindings.createBooleanBinding(() -> codeArea.getText().isEmpty(), codeArea.textProperty()));
		codeArea.focusTraversableProperty()
		        .bind(Bindings.createBooleanBinding(() -> !codeArea.getText().isEmpty(), codeArea.textProperty()));
		codeAreaHelper.initializeJavaEditor(codeArea);
	}

	private void loadChildren(TreeItem<ClassField> parent, ClassFields classFields) {
		final List<TreeItem<ClassField>> newTableItems = Arrays.stream(classFields.getFields())
		                                                       .map(TreeItem::new)
		                                                       .peek(this::handleWrappedObject)
		                                                       .collect(Collectors.toList());
		parent.getChildren().setAll(newTableItems);
	}

	private void handleWrappedObject(TreeItem<ClassField> treeItem) {
		if (!(treeItem.getValue().getValue() instanceof WrappedObject)) {
			return;
		}
		// Check if the field is another object, so we can
		// handle loading that object's fields
		treeItem.getChildren().add(new TreeItem<>());
		// On expand, load the nested fields
		// On collapse, empty the nested fields
		treeItem.expandedProperty().addListener((obs, old, newv) -> {
			if (newv) {
				final RunningJvm selectedJvm = currentJvm.get();
				if (selectedJvm == null) {
					return;
				}
				final ClassFieldKey[] classFieldKeys = fieldTreeHelper.getClassFieldKeyPath(treeItem);
				final ClassLoaderDescriptor classLoaderDescriptor = currentClass.get()
				                                                                .getLoadedClass()
				                                                                .getClassLoaderDescriptor();
				final ClassFieldPath classFieldPath = new ClassFieldPath(classFieldKeys, classLoaderDescriptor);
				executorService.submit(() -> {
					final ClassFields classFieldsResponse = clientHandler.getFields(selectedJvm, classFieldPath);
					Platform.runLater(() -> loadChildren(treeItem, classFieldsResponse));
				});
			}
			else {
				treeItem.getChildren().clear();
				treeItem.getChildren().add(new TreeItem<>());
			}
		});
	}

}
