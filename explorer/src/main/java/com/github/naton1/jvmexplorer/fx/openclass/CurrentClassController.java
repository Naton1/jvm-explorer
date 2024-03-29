package com.github.naton1.jvmexplorer.fx.openclass;

import com.github.naton1.jvmexplorer.agent.AgentException;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.bytecode.Assembler;
import com.github.naton1.jvmexplorer.bytecode.AssemblyException;
import com.github.naton1.jvmexplorer.bytecode.BytecodeTextifier;
import com.github.naton1.jvmexplorer.bytecode.OpenJdkJasmAssembler;
import com.github.naton1.jvmexplorer.bytecode.OpenJdkJasmDisassembler;
import com.github.naton1.jvmexplorer.bytecode.QuiltflowerDecompiler;
import com.github.naton1.jvmexplorer.bytecode.compile.CompileResult;
import com.github.naton1.jvmexplorer.bytecode.compile.Compiler;
import com.github.naton1.jvmexplorer.bytecode.compile.JavacBytecodeProvider;
import com.github.naton1.jvmexplorer.bytecode.compile.RemoteJavacBytecodeProvider;
import com.github.naton1.jvmexplorer.fx.TreeViewPlaceholderSkin;
import com.github.naton1.jvmexplorer.fx.classes.ClassTreeNode;
import com.github.naton1.jvmexplorer.fx.classes.FilterableTreeItem;
import com.github.naton1.jvmexplorer.fx.method.ModifyMethodController;
import com.github.naton1.jvmexplorer.helper.AcceleratorHelper;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.AsmHelper;
import com.github.naton1.jvmexplorer.helper.ClassTreeHelper;
import com.github.naton1.jvmexplorer.helper.CodeAreaHelper;
import com.github.naton1.jvmexplorer.helper.DialogHelper;
import com.github.naton1.jvmexplorer.helper.EditorHelper;
import com.github.naton1.jvmexplorer.helper.FieldTreeHelper;
import com.github.naton1.jvmexplorer.helper.HighlightHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.protocol.PatchResult;
import com.github.naton1.jvmexplorer.protocol.WrappedObject;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class CurrentClassController {

	private static final String NO_CLASS_FILE_OPEN = "Select a loaded class to open";
	private static final String PROCESSOR_FAILED = "Processor failed";

	private final EditorHelper editorHelper = new EditorHelper();
	private final FieldTreeHelper fieldTreeHelper = new FieldTreeHelper();
	private final ClassTreeHelper classTreeHelper = new ClassTreeHelper();

	private final SimpleStringProperty decompiledClass = new SimpleStringProperty();
	private final SimpleStringProperty disassembledClass = new SimpleStringProperty();

	private final SimpleBooleanProperty allowBytecodeEditing = new SimpleBooleanProperty(false);
	private final SimpleBooleanProperty allowClassFileEditing = new SimpleBooleanProperty(false);

	@FXML
	private CodeArea bytecode;

	@FXML
	private CodeArea classFile;

	@FXML
	private TreeView<ClassField> classFields;

	@FXML
	private TitledPane loadedClassTitlePane;

	@FXML
	private Tab bytecodeTab;

	@FXML
	private Tab classFileTab;

	private ObjectProperty<RunningJvm> currentJvm;
	private ObjectProperty<ClassContent> currentClass;

	private ScheduledExecutorService executorService;
	private ClientHandler clientHandler;
	private AlertHelper alertHelper;

	private CodeAreaHelper codeAreaHelper;

	private FilterableTreeItem<ClassTreeNode> classesTreeRoot;
	private Consumer<TreeItem<ClassTreeNode>> handleSelection;

	public void initialize(Stage stage, ScheduledExecutorService executorService, ClientHandler clientHandler,
	                       ObjectProperty<RunningJvm> currentJvm, ObjectProperty<ClassContent> currentClass,
	                       FilterableTreeItem<ClassTreeNode> classesTreeRoot,
	                       Consumer<TreeItem<ClassTreeNode>> handleSelection) {
		this.executorService = executorService;
		this.clientHandler = clientHandler;
		this.currentJvm = currentJvm;
		this.currentClass = currentClass;
		this.alertHelper = new AlertHelper(stage);
		this.codeAreaHelper = new CodeAreaHelper(executorService);
		this.classesTreeRoot = classesTreeRoot;
		this.handleSelection = handleSelection;
		initialize();
	}

	private void initialize() {

		setupTitlePaneText();

		currentClass.addListener((obs, old, newv) -> onClassChange(old, newv));

		setupClassFieldTree();

		setupCodeArea(classFile);
		setupCodeArea(bytecode);

		setupBytecodeEditor();
		setupJavaEditor();
	}

	private void setupBytecodeEditor() {
		setupEditor(bytecode, disassembledClass, allowBytecodeEditing, bytecodeTab, this::onBytecodeSave);
	}

	private void refreshClass() {
		final RunningJvm selectedJvm = currentJvm.get();
		if (selectedJvm == null) {
			return;
		}
		final ClassContent initialClassContent = currentClass.get();
		if (initialClassContent == null) {
			return;
		}
		final LoadedClass loadedClass = initialClassContent.getLoadedClass();
		log.debug("Refreshing class: {}", loadedClass);
		executorService.submit(() -> {
			final ClassContent classContent = clientHandler.getClassContent(selectedJvm, loadedClass);
			log.debug("Refreshed class content for {}", loadedClass);
			if (classContent != null) {
				Platform.runLater(() -> {
					final ClassContent currentClassContent = currentClass.get();
					if (currentClassContent == null || !currentClassContent.getLoadedClass().equals(loadedClass)) {
						log.debug("Current class changed from {} to {}, ignoring replacement",
						          loadedClass,
						          classContent);
						return;
					}
					currentClass.set(classContent);
				});
			}
		});
	}

	private void onBytecodeSave(RunningJvm runningJvm, LoadedClass loadedClass, String text) {
		executorService.submit(() -> {
			final Assembler assembler = new OpenJdkJasmAssembler();
			try {
				final byte[] assembledClassFile = assembler.assemble(text);
				final PatchResult result = clientHandler.replaceClass(runningJvm, loadedClass, assembledClassFile);
				Platform.runLater(() -> {
					if (!result.isSuccess()) {
						alertHelper.showError("Class Patch Failed", result.getMessage());
						return;
					}
					disassembledClass.set(text);
					refreshClass();
				});
			}
			catch (AssemblyException assemblyException) {
				Platform.runLater(() -> alertHelper.showError("Assembly Failed",
				                                              "Failed to assemble class",
				                                              assemblyException));
			}
		});
	}

	private void setupJavaEditor() {
		setupEditor(classFile, decompiledClass, allowClassFileEditing, classFileTab, this::onClassFileSave);

		final ContextMenu contextMenu = classFile.getContextMenu();
		final KeyCodeCombination accelerator = new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN);
		final MenuItem modifyMethod = new MenuItem("Modify Method");
		modifyMethod.setAccelerator(accelerator);
		modifyMethod.setOnAction(e -> showModifyMethod());
		AcceleratorHelper.process(classFile, accelerator, modifyMethod);
		contextMenu.getItems().add(modifyMethod);
	}

	private List<LoadedClass> getClassPath(LoadedClass loadedClass) {
		final ClassLoaderDescriptor classLoaderDescriptor = loadedClass.getClassLoaderDescriptor();
		final TreeItem<ClassTreeNode> classLoaderTreeItem;
		if (classLoaderDescriptor == null) {
			classLoaderTreeItem = null;
		}
		else {
			classLoaderTreeItem = classesTreeRoot.streamSourceItems()
			                                     .filter(c -> c.getValue() != null)
			                                     .filter(c -> c.getValue().getType() == ClassTreeNode.Type.CLASSLOADER)
			                                     .filter(c -> c.getValue()
			                                                   .getClassLoaderDescriptor()
			                                                   .equals(classLoaderDescriptor))
			                                     .findFirst()
			                                     .orElse(null);
		}
		return classTreeHelper.getLoadedClassScope(classesTreeRoot, classLoaderTreeItem);
	}

	private void showModifyMethod() {
		try {
			final Dialog<ButtonType> dialog = new Dialog<>();
			final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader()
			                                                   .getResource("fxml/modify_method.fxml"));
			final Parent root = loader.load();
			final ModifyMethodController modifyMethodController = loader.getController();
			final ClassContent classContent = currentClass.get();
			if (classContent == null) {
				return;
			}
			final RunningJvm runningJvm = currentJvm.get();
			if (runningJvm == null) {
				return;
			}
			final LoadedClass loadedClass = classContent.getLoadedClass();
			final List<LoadedClass> classpath = getClassPath(loadedClass);
			dialog.getDialogPane().setContent(root);
			dialog.setTitle("Modify Method");
			dialog.initOwner(classFile.getScene().getWindow());
			DialogHelper.initCustomDialog(dialog, currentJvm);
			final Consumer<Boolean> closeHandler = success -> {
				dialog.getDialogPane().getScene().getWindow().hide();
				if (success) {
					refreshClass();
				}
			};
			modifyMethodController.initialize(executorService,
			                                  clientHandler,
			                                  runningJvm,
			                                  loadedClass,
			                                  closeHandler,
			                                  classpath,
			                                  classContent.getClassContent());
			dialog.show();
		}
		catch (IOException e) {
			log.warn("Failed to initialize code executor", e);
		}
	}

	private void onClassFileSave(RunningJvm runningJvm, LoadedClass loadedClass, String text) {
		final List<LoadedClass> classpath = getClassPath(loadedClass);
		executorService.submit(() -> {
			final Compiler compiler = new Compiler();
			final int javaVersion = getJavaVersion(runningJvm);
			final JavacBytecodeProvider javacBytecodeProvider = new RemoteJavacBytecodeProvider(clientHandler,
			                                                                                    runningJvm,
			                                                                                    classpath);
			final CompileResult compileResult = compiler.compile(javaVersion,
			                                                     loadedClass.getName(),
			                                                     text,
			                                                     javacBytecodeProvider);
			if (compileResult.isSuccess()) {
				final PatchResult patchResult = clientHandler.replaceClass(runningJvm,
				                                                           loadedClass,
				                                                           compileResult.getClassContent());
				if (patchResult.isSuccess()) {
					Platform.runLater(() -> {
						decompiledClass.set(text);
						refreshClass();
					});
				}
				else {
					Platform.runLater(() -> alertHelper.showError("Patch Failed", patchResult.getMessage()));
				}
			}
			else {
				Platform.runLater(() -> alertHelper.showError("Compilation Failed", compileResult.getStdOut()));
			}
		});
	}

	private int getJavaVersion(RunningJvm runningJvm) {
		try {
			return runningJvm.getJavaVersion();
		}
		catch (AgentException e) {
			log.warn("Failed to get java version for remote code execution", e);
			return Runtime.version().feature();
		}
	}

	private void setupEditor(CodeArea editor, SimpleStringProperty editorBaseContent,
	                         SimpleBooleanProperty allowEditing, Tab header, OnSaveHandler onSave) {

		editor.editableProperty().bind(allowEditing);

		editor.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			if (!e.isControlDown()) {
				return;
			}
			final int position = editor.getCaretPosition();
			final String s = editor.getText();
			openClassAtCursor(position, s);
		});

		final BooleanBinding editorModified = Bindings.createBooleanBinding(() -> {
			if (!allowEditing.get()) {
				return false;
			}
			if (currentClass.get() == null) {
				return false;
			}
			final String baseContent = editorBaseContent.get();
			if (baseContent == null) {
				return false;
			}
			// Normalize line endings
			final List<String> editorText = editor.getText().lines().collect(Collectors.toList());
			final List<String> baseLines = baseContent.lines().collect(Collectors.toList());
			return !editorText.equals(baseLines);
		}, editor.textProperty(), allowEditing, editorBaseContent, currentClass);

		final String baseTabTitle = header.getText();
		header.textProperty()
		      .bind(Bindings.createStringBinding(() -> baseTabTitle + (editorModified.get() ? "*" : ""),
		                                         editorModified));

		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem save = new MenuItem("Save Changes");
		save.disableProperty().bind(editorModified.not());

		save.setOnAction(e -> {
			final RunningJvm runningJvm = currentJvm.get();
			if (runningJvm == null) {
				return;
			}
			final ClassContent classContent = currentClass.get();
			if (classContent == null) {
				return;
			}
			final LoadedClass loadedClass = classContent.getLoadedClass();
			final String text = editor.getText();
			onSave.onSave(runningJvm, loadedClass, text);
		});

		final KeyCodeCombination shortcut = new KeyCodeCombination(KeyCode.S, KeyCodeCombination.CONTROL_DOWN);
		save.setAccelerator(shortcut);

		AcceleratorHelper.process(editor, shortcut, save);

		final MenuItem reset = new MenuItem("Reset Changes");
		reset.disableProperty().bind(editorModified.not());

		reset.setOnAction(e -> editor.replaceText(editorBaseContent.get()));

		contextMenu.getItems().addAll(save, reset);

		editor.setContextMenu(contextMenu);
	}

	private void openClassAtCursor(int cursorPosition, String text) {
		if (Character.isJavaIdentifierPart(text.charAt(cursorPosition))) {

			// Find the left-most character
			int startIndex = cursorPosition;
			while (startIndex >= 0 && Character.isJavaIdentifierPart(text.charAt(startIndex))) {
				startIndex--;
			}
			startIndex++;

			// Find the start
			while (startIndex < text.length() && !Character.isJavaIdentifierStart(text.charAt(startIndex))) {
				startIndex++;
			}

			// Find the right-most character
			int endIndex = cursorPosition;
			while (endIndex < text.length() && Character.isJavaIdentifierPart(text.charAt(endIndex))) {
				endIndex++;
			}
			endIndex--;

			if (startIndex >= endIndex) {
				// Invalid
				return;
			}

			final String javaName = text.substring(startIndex, endIndex + 1);
			log.debug("Found java name at mouse click: {}", javaName);

			final TreeItem<ClassTreeNode> correspondingClass = classesTreeRoot.streamSourceItems()
			                                                                  .filter(f -> f.getValue() != null)
			                                                                  .filter(f -> f.getValue().getType()
			                                                                               == ClassTreeNode.Type.CLASS)
			                                                                  .filter(f -> f.getValue()
			                                                                                .getLoadedClass()
			                                                                                .getSimpleName()
			                                                                                .equals(javaName))
			                                                                  .findFirst()
			                                                                  .orElse(null);

			if (correspondingClass == null) {
				return;
			}

			log.debug("Found {}", correspondingClass.getValue());
			handleSelection.accept(correspondingClass);
		}
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
		allowClassFileEditing.set(false);
		allowBytecodeEditing.set(false);
		if (newv == null) {
			classFile.clear();
			bytecode.clear();
			classFields.getRoot().getChildren().clear();
		}
		else {
			processBytecode(newv, new QuiltflowerDecompiler(), classFile, newDecompiledClass -> {
				allowClassFileEditing.set(!PROCESSOR_FAILED.equals(newDecompiledClass));
				decompiledClass.set(newDecompiledClass);
			});
			processBytecode(newv, new OpenJdkJasmDisassembler(), bytecode, newDisassembledClass -> {
				allowBytecodeEditing.set(!PROCESSOR_FAILED.equals(newDisassembledClass));
				disassembledClass.set(newDisassembledClass);
			});
			loadChildren(classFields.getRoot(), newv.getClassFields());
			loadHighlightContext(newv.getClassContent());
		}
	}

	private void loadHighlightContext(byte[] newClass) {
		final ClassNode classNode = AsmHelper.parse(newClass,
		                                            ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE
		                                            | ClassReader.SKIP_DEBUG);
		final String propName = HighlightHelper.HighlightContext.class.getName();
		final HighlightHelper.HighlightContext context = HighlightHelper.createContextFor(classNode);
		bytecode.getProperties().put(propName, context);
		classFile.getProperties().put(propName, context);
	}

	private void processBytecode(ClassContent classContent, BytecodeTextifier bytecodeTextifier, CodeArea codeArea,
	                             Consumer<String> onProcess) {
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
			final String processedClass = process(bytecodeTextifier, classContent.getClassContent());
			log.debug("Finished processing class {} with {}", classContent.getLoadedClass(), bytecodeTextifier);
			processingPlaceholderTask.cancel(false);
			Platform.runLater(() -> {
				// Don't update if the class was switched again
				if (!classContent.equals(currentClass.get())) {
					log.debug("Class updated; not showing output for {} with processor {}",
					          classContent.getLoadedClass(),
					          bytecodeTextifier);
					return;
				}
				final String newText;
				if (processedClass == null || processedClass.isEmpty()) {
					log.warn("Process result is empty for class {} with processor {}",
					         classContent.getLoadedClass(),
					         bytecodeTextifier);
					newText = PROCESSOR_FAILED;
				}
				else {
					newText = processedClass;
				}
				codeArea.replaceText(newText);
				codeArea.getUndoManager().forgetHistory(); // it's a new class, reset the history
				// sometimes the text area wasn't scrolling to 0 by default, so let's tell it to no matter what
				codeArea.scrollYToPixel(0);
				// Trigger update immediately
				codeAreaHelper.triggerHighlightUpdate(codeArea);
				onProcess.accept(newText);
				log.debug("Finished applying processor result for {} with processor {}",
				          classContent.getLoadedClass(),
				          bytecodeTextifier);
			});
		});
	}

	private String process(BytecodeTextifier bytecodeTextifier, byte[] input) {
		if (input == null || input.length == 0) {
			log.warn("No input to process for {}", bytecodeTextifier);
			return null;
		}
		try {
			return bytecodeTextifier.process(input);
		}
		catch (Throwable t) {
			log.warn("Failed to process bytecode using {}", bytecodeTextifier, t);
			return null;
		}
	}

	private void setupClassFieldTree() {
		final TreeItem<ClassField> classFieldRoot = new TreeItem<>(null);
		classFields.setShowRoot(false);
		classFields.setRoot(classFieldRoot);

		classFields.setCellFactory(new ClassFieldCellFactory(editorHelper,
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

		final TreeViewPlaceholderSkin<ClassField> treeViewPlaceholderSkin = new TreeViewPlaceholderSkin<>(classFields);
		treeViewPlaceholderSkin.placeholderProperty().setValue(classFieldsPlaceholder);
		classFields.setSkin(treeViewPlaceholderSkin);
	}

	// CodeArea must be in a VBox to replace and insert into VirtualizedScrollPane
	private void setupCodeArea(CodeArea codeArea) {
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
		                                                       // Remove cycles
		                                                       .filter(field -> !Objects.equals(parent.getValue(),
		                                                                                        field))
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

	private interface OnSaveHandler {
		void onSave(RunningJvm runningJvm, LoadedClass loadedClass, String text);
	}

}
