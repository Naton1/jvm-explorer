package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.fx.compile.RemoteCodeExecutorController;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ClassTreeHelper;
import com.github.naton1.jvmexplorer.helper.ExportHelper;
import com.github.naton1.jvmexplorer.helper.FileHelper;
import com.github.naton1.jvmexplorer.helper.PatchHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class ClassCellFactory implements Callback<TreeView<ClassTreeNode>, TreeCell<ClassTreeNode>> {

	private final PatchHelper patchHelper = new PatchHelper();
	private final ClassTreeHelper classTreeHelper = new ClassTreeHelper();
	private final FileHelper fileHelper = new FileHelper();

	private final ExecutorService executorService;
	private final AlertHelper alertHelper;
	private final ObjectProperty<RunningJvm> currentJvm;
	private final ClientHandler clientHandler;
	private final FilterableTreeItem<ClassTreeNode> classesTreeRoot;
	private final ExportHelper exportHelper;
	private final Consumer<RunningJvm> onLoadClasses;
	private final JvmExplorerSettings settings;

	@Override
	public TreeCell<ClassTreeNode> call(TreeView<ClassTreeNode> classes) {
		final TreeCell<ClassTreeNode> treeCell = new TreeCell<>();
		setupImageBinding(treeCell);
		setupTextBinding(treeCell);
		setupTooltipBinding(treeCell);
		setupContextMenu(treeCell, classes);
		return treeCell;
	}

	private void setupImageBinding(TreeCell<ClassTreeNode> treeCell) {
		treeCell.graphicProperty().bind(Bindings.createObjectBinding(() -> {
			final ClassTreeNode item = treeCell.getItem();
			if (item == null) {
				return null;
			}
			return new ImageView(item.getType().getImage());
		}, treeCell.itemProperty()));
	}

	private void setupTextBinding(TreeCell<ClassTreeNode> treeCell) {
		treeCell.textProperty()
		        .bind(Bindings.when(treeCell.itemProperty().isNotNull())
		                      .then(treeCell.itemProperty().asString())
		                      .otherwise(""));
	}

	private void setupTooltipBinding(TreeCell<ClassTreeNode> treeCell) {
		final Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(Bindings.createStringBinding(() -> {
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode == null) {
				return "";
			}
			switch (classTreeNode.getType()) {
			case CLASSLOADER:
				return classTreeNode.getClassLoaderDescriptor().getDescription();
			case PACKAGE:
				return classTreeNode.getPackageSegment();
			case CLASS:
				return classTreeNode.getLoadedClass().toString();
			default:
				log.warn("Unknown type: {}", classTreeNode.getType());
				return "";
			}
		}, treeCell.itemProperty()));
		treeCell.tooltipProperty()
		        .bind(Bindings.when(treeCell.itemProperty().isNotNull()).then(tooltip).otherwise((Tooltip) null));
	}

	private void setupContextMenu(TreeCell<ClassTreeNode> treeCell, TreeView<ClassTreeNode> classes) {
		final ContextMenu classesContextMenu = new ContextMenu();

		final MenuItem scopedExport = createScopedExport(treeCell, classes);
		final MenuItem exportClasses = createExportClasses(classes);
		final MenuItem reloadClasses = createReloadClasses();
		final MenuItem scopedReplace = createScopedReplace(treeCell, classes);
		final MenuItem replaceClasses = createReplaceClasses(classes);
		final MenuItem includeClassLoader = createShowClassLoader(reloadClasses);
		final MenuItem executeCode = createExecuteCode(treeCell, classes);

		treeCell.itemProperty().addListener((obs, old, newv) -> {
			classesContextMenu.getItems().clear();
			if (newv != null) {
				classesContextMenu.getItems().addAll(scopedExport, scopedReplace, new SeparatorMenuItem());
			}
			classesContextMenu.getItems()
			                  .addAll(executeCode,
			                          new SeparatorMenuItem(),
			                          exportClasses,
			                          replaceClasses,
			                          reloadClasses,
			                          new SeparatorMenuItem(),
			                          includeClassLoader);
		});

		treeCell.setContextMenu(classesContextMenu);
	}

	private MenuItem createShowClassLoader(MenuItem reloadClasses) {
		final CheckMenuItem includeClassLoader = new CheckMenuItem("Show Class Loaders");
		includeClassLoader.setOnAction(e -> {
			settings.getShowClassLoader().set(includeClassLoader.isSelected());
			settings.save(JvmExplorerSettings.DEFAULT_SETTINGS_FILE);
			final RunningJvm runningJvm = currentJvm.get();
			if (runningJvm == null) {
				return;
			}
			// Reload classes, if applicable
			reloadClasses.getOnAction().handle(e);
		});
		settings.getShowClassLoader().addListener((obs, old, newv) -> includeClassLoader.setSelected(newv));
		includeClassLoader.setSelected(settings.getShowClassLoader().getValue());
		return includeClassLoader;
	}

	private MenuItem createReplaceClasses(TreeView<ClassTreeNode> classes) {
		final MenuItem replaceClasses = new MenuItem("Replace Classes");
		replaceClasses.setOnAction(e -> {
			final RunningJvm jvm = currentJvm.get();
			if (jvm == null) {
				return;
			}
			final File selectedFile = selectImportJarFile(classes.getScene().getWindow());
			if (selectedFile == null) {
				return;
			}
			executorService.submit(() -> replaceClasses(selectedFile, jvm, null));
		});
		return replaceClasses;
	}

	private MenuItem createScopedReplace(TreeCell<ClassTreeNode> treeCell, TreeView<ClassTreeNode> classes) {
		final MenuItem scopedReplace = new MenuItem("Replace Class");
		scopedReplace.textProperty().bind(Bindings.createStringBinding(() -> {
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode == null) {
				return "";
			}
			switch (classTreeNode.getType()) {
			case CLASSLOADER:
				return "Replace Class Loader";
			case PACKAGE:
				return "Replace Package";
			case CLASS:
				return "Replace Class";
			}
			log.warn("Unknown type: {}", classTreeNode.getType());
			return "";
		}, treeCell.itemProperty()));
		scopedReplace.setOnAction(e -> {
			final RunningJvm jvm = currentJvm.get();
			if (jvm == null) {
				return;
			}
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode == null) {
				return;
			}
			switch (classTreeNode.getType()) {
			case CLASSLOADER:
				final File importClassLoader = selectImportJarFile(classes.getScene().getWindow());
				if (importClassLoader == null) {
					return;
				}
				final ClassLoaderDescriptor classLoaderDescriptor = classTreeNode.getClassLoaderDescriptor();
				executorService.submit(() -> replaceClasses(importClassLoader, jvm, classLoaderDescriptor));
				break;
			case PACKAGE:
				final File importPackage = selectImportJarFile(classes.getScene().getWindow());
				if (importPackage == null) {
					return;
				}
				final ClassLoaderDescriptor packageClassLoader =
						classTreeHelper.getNodeClassLoader(treeCell.getTreeItem());
				executorService.submit(() -> replaceClasses(importPackage, jvm, packageClassLoader));
				break;
			case CLASS:
				final File selectedFile = selectImportClassFile(classes.getScene().getWindow());
				if (selectedFile == null) {
					return;
				}
				executorService.submit(() -> replaceClass(selectedFile, classTreeNode.getLoadedClass(), jvm));
				break;
			}
		});
		return scopedReplace;
	}

	private MenuItem createReloadClasses() {
		final MenuItem reloadClasses = new MenuItem("Refresh Classes");
		reloadClasses.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			classesTreeRoot.getSourceChildren().clear();
			// I think my java version is broken... it can't compile obvious things like this without casting
			executorService.submit((Runnable) () -> onLoadClasses.accept(activeJvm));
		});
		return reloadClasses;
	}

	private MenuItem createExportClasses(TreeView<ClassTreeNode> classes) {
		final MenuItem exportClasses = new MenuItem("Export Classes");
		exportClasses.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			final File selectedFile = selectExportJarFile(activeJvm.getName(), classes.getScene().getWindow());
			if (selectedFile == null) {
				return;
			}
			final List<LoadedClass> loadedClasses = classesTreeRoot.streamVisible()
			                                                       .map(ClassTreeNode::getLoadedClass)
			                                                       .filter(Objects::nonNull)
			                                                       .collect(Collectors.toList());
			executorService.submit(() -> export(selectedFile, loadedClasses, activeJvm));
		});
		return exportClasses;
	}

	private MenuItem createScopedExport(TreeCell<ClassTreeNode> treeCell, TreeView<ClassTreeNode> classes) {
		final MenuItem scopedExport = new MenuItem();
		scopedExport.textProperty().bind(Bindings.createStringBinding(() -> {
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode == null) {
				return "";
			}
			switch (classTreeNode.getType()) {
			case CLASSLOADER:
				return "Export Class Loader";
			case PACKAGE:
				return "Export Package";
			case CLASS:
				return "Export Class";
			}
			log.warn("Unknown type: {}", classTreeNode.getType());
			return "";
		}, treeCell.itemProperty()));
		scopedExport.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode == null) {
				return;
			}
			switch (classTreeNode.getType()) {
			case CLASSLOADER:
				final File exportClassLoader = selectExportJarFile(classTreeNode.getPackageSegment(),
				                                                   classes.getScene().getWindow());
				if (exportClassLoader == null) {
					return;
				}
				final ClassLoaderDescriptor classLoaderDescriptor = classTreeNode.getClassLoaderDescriptor();
				log.debug("Exporting class loader: {}", classLoaderDescriptor);
				final List<LoadedClass> classesInClassLoader = classTreeHelper.getClassesInPackage(classesTreeRoot,
				                                                                                   "",
				                                                                                   classLoaderDescriptor);
				executorService.submit(() -> export(exportClassLoader, classesInClassLoader, activeJvm));
				break;
			case PACKAGE:
				final File exportPackage = selectExportJarFile(classTreeNode.getPackageSegment(),
				                                               classes.getScene().getWindow());
				if (exportPackage == null) {
					return;
				}
				final String fullPackageName = classTreeHelper.getPackageName(treeCell.getTreeItem());
				final ClassLoaderDescriptor packageClassLoader = this.settings.getShowClassLoader().get()
				                                                 ?
				                                                 classTreeHelper.getNodeClassLoader(treeCell.getTreeItem())
				                                                 : null;
				log.debug("Exporting package: {} in classloader: {}", fullPackageName, packageClassLoader);
				final List<LoadedClass> classesInPackage = classTreeHelper.getClassesInPackage(classesTreeRoot,
				                                                                               fullPackageName,
				                                                                               packageClassLoader);
				executorService.submit(() -> export(exportPackage, classesInPackage, activeJvm));
				break;
			case CLASS:
				final LoadedClass loadedClass = classTreeNode.getLoadedClass();
				final File selectedFile = selectExportClassFile(loadedClass.getSimpleName() + ".class",
				                                                classes.getScene().getWindow());
				if (selectedFile == null) {
					return;
				}
				log.debug("Exporting class: {}", loadedClass);
				executorService.submit(() -> export(selectedFile, loadedClass, activeJvm));
				break;
			}
		});
		return scopedExport;
	}

	private MenuItem createExecuteCode(TreeCell<ClassTreeNode> treeCell, TreeView<ClassTreeNode> treeView) {
		final MenuItem executeCode = new MenuItem();
		executeCode.textProperty().bind(Bindings.createStringBinding(() -> {
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode != null) {
				switch (classTreeNode.getType()) {
				case CLASSLOADER:
					return "Run Code In Class Loader";
				case PACKAGE:
				case CLASS:
					return "Run Code In Package";
				}
				log.warn("Unknown type: {}", classTreeNode.getType());
			}
			return "Run Code";
		}, treeCell.itemProperty()));
		executeCode.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			final Window owner = treeView.getScene().getWindow();
			final ClassTreeNode classTreeNode = treeCell.getItem();
			if (classTreeNode == null) {
				final List<LoadedClass> classpath = classTreeHelper.getLoadedClassScope(classesTreeRoot, null);
				executeCode(owner, classpath, null, null);
				return;
			}
			switch (classTreeNode.getType()) {
			case CLASSLOADER:
				final List<LoadedClass> classLoaderScope = classTreeHelper.getLoadedClassScope(classesTreeRoot,
				                                                                               treeCell.getTreeItem());
				final ClassLoaderDescriptor classLoaderDescriptor = classTreeNode.getClassLoaderDescriptor();
				executeCode(owner, classLoaderScope, classLoaderDescriptor, null);
				break;
			case PACKAGE:
				final String packageName = classTreeHelper.getPackageName(treeCell.getTreeItem());
				final TreeItem<ClassTreeNode> packageClassLoaderNodeItem = classTreeHelper.getNodeClassLoaderTreeItem(
						treeCell.getTreeItem());
				final ClassLoaderDescriptor packageClassLoader;
				if (packageClassLoaderNodeItem != null) {
					packageClassLoader = packageClassLoaderNodeItem.getValue().getClassLoaderDescriptor();
				}
				else {
					final FilterableTreeItem<ClassTreeNode> node =
							((FilterableTreeItem<ClassTreeNode>) treeCell.getTreeItem());
					packageClassLoader = node.streamSource()
					                         .filter(c -> c.getType() == ClassTreeNode.Type.CLASS)
					                         .map(ClassTreeNode::getLoadedClass)
					                         .map(LoadedClass::getClassLoaderDescriptor)
					                         .filter(Objects::nonNull)
					                         .findFirst()
					                         .orElse(null);
				}
				final List<LoadedClass> packageClassPath = classTreeHelper.getLoadedClassScope(classesTreeRoot,
				                                                                               packageClassLoaderNodeItem);
				executeCode(owner, packageClassPath, packageClassLoader, packageName);
				break;
			case CLASS:
				final TreeItem<ClassTreeNode> packageNode = treeCell.getTreeItem().getParent();
				final String classPackageName = packageNode != null && packageNode.getValue() != null
				                                && packageNode.getValue().getType() == ClassTreeNode.Type.PACKAGE
				                                ? classTreeHelper.getPackageName(packageNode) : null;
				final TreeItem<ClassTreeNode> classLoaderNodeItem =
						classTreeHelper.getNodeClassLoaderTreeItem(treeCell.getTreeItem());
				final ClassLoaderDescriptor classLoader = treeCell.getTreeItem()
				                                                  .getValue()
				                                                  .getLoadedClass()
				                                                  .getClassLoaderDescriptor();
				final List<LoadedClass> classPath = classTreeHelper.getLoadedClassScope(classesTreeRoot,
				                                                                        classLoaderNodeItem);
				executeCode(owner, classPath, classLoader, classPackageName);
				break;
			}
		});
		return executeCode;
	}

	private File selectImportJarFile(Window owner) {
		return fileHelper.openJar(owner, "Replace Classes");
	}

	private File selectImportClassFile(Window owner) {
		return fileHelper.openClass(owner, "Replace Class");
	}

	private File selectExportClassFile(String initialFileName, Window owner) {
		return fileHelper.saveClass(owner, "Export Class", initialFileName);
	}

	private File selectExportJarFile(String initialFileName, Window owner) {
		return fileHelper.saveJar(owner, "Export Classes", initialFileName);
	}

	private void export(File selectedFile, LoadedClass loadedClass, RunningJvm activeJvm) {
		try {
			final byte[] classContent = clientHandler.getClassBytes(activeJvm, loadedClass);
			Files.write(selectedFile.toPath(), classContent);
		}
		catch (IOException ex) {
			log.warn("Failed to write class file {}", loadedClass.getName(), ex);
		}
	}

	private void export(File selectedFile, List<LoadedClass> classes, RunningJvm activeJvm) {
		final File exportParentFile = selectedFile.getParentFile();
		if (exportParentFile != null) {
			exportParentFile.mkdirs();
		}
		final SimpleIntegerProperty progress = new SimpleIntegerProperty(0);
		final SimpleBooleanProperty isComplete = new SimpleBooleanProperty(false);
		final SimpleBooleanProperty success = new SimpleBooleanProperty(false);
		Platform.runLater(() -> {
			final StringBinding titleText = Bindings.when(isComplete)
			                                        .then("Export Finished")
			                                        .otherwise("Export In Progress");
			final StringBinding contentText = Bindings.createStringBinding(() -> {
				if (isComplete.get()) {
					return "Export " + (success.get() ? "succeeded" : "failed");
				}
				else {
					return "Creating JAR: " + progress.get() + " classes";
				}
			}, isComplete, success, progress);
			alertHelper.showObservableInfo(titleText, contentText);
		});
		final boolean result = exportHelper.export(activeJvm,
		                                           classes,
		                                           selectedFile,
		                                           currentExportProgress -> Platform.runLater(() -> progress.set(
				                                           currentExportProgress)));
		Platform.runLater(() -> {
			isComplete.set(true);
			success.set(result);
		});
	}

	private void replaceClass(File selectedFile, LoadedClass loadedClass, RunningJvm activeJvm) {
		final byte[] contents;
		try {
			contents = Files.readAllBytes(selectedFile.toPath());
		}
		catch (IOException ex) {
			log.warn("Failed to read file", ex);
			return;
		}
		final boolean replaced = clientHandler.replaceClass(activeJvm, loadedClass, contents);
		Platform.runLater(() -> {
			final Alert.AlertType alertType = replaced ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
			final String title = replaced ? "Replaced Class" : "Replace Failed";
			final String header = replaced ? "Successfully replaced class" : "Class replacement failed";
			alertHelper.show(alertType, title, header);
		});
	}

	private void replaceClasses(File selectedFile, RunningJvm activeJvm, ClassLoaderDescriptor classLoaderDescriptor) {
		final SimpleIntegerProperty progress = new SimpleIntegerProperty(0);
		final SimpleBooleanProperty isComplete = new SimpleBooleanProperty(false);
		final SimpleBooleanProperty success = new SimpleBooleanProperty(false);
		Platform.runLater(() -> {
			final StringBinding titleText = Bindings.when(isComplete)
			                                        .then("Patching Finished")
			                                        .otherwise("Patch In Progress");
			final StringBinding contentText = Bindings.createStringBinding(() -> {
				if (isComplete.get()) {
					return "Patch " + (success.get() ? "succeeded" : "failed");
				}
				else {
					return "Patching: " + progress.get() + " classes";
				}
			}, isComplete, success, progress);
			alertHelper.showObservableInfo(titleText, contentText);
		});
		final boolean result = patchHelper.patch(selectedFile,
		                                         activeJvm,
		                                         clientHandler,
		                                         classLoaderDescriptor,
		                                         currentExportProgress -> Platform.runLater(() -> progress.set(
				                                         currentExportProgress)));
		Platform.runLater(() -> {
			isComplete.set(true);
			success.set(result);
		});
	}

	private void executeCode(Window owner, List<LoadedClass> classpath, ClassLoaderDescriptor classLoaderDescriptor,
	                         String packageName) {
		try {
			final Dialog<?> dialog = new Dialog<>();
			final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader()
			                                                   .getResource("fxml/remote_code_executor.fxml"));
			final Parent root = loader.load();
			final RemoteCodeExecutorController remoteCodeExecutorController = loader.getController();
			remoteCodeExecutorController.initialize(executorService,
			                                        clientHandler,
			                                        currentJvm.get(),
			                                        classLoaderDescriptor,
			                                        packageName,
			                                        classpath);
			dialog.getDialogPane().setContent(root);
			dialog.getDialogPane().getButtonTypes().setAll();
			dialog.getDialogPane().getStyleClass().add("custom-dialog-pane");
			final String title = Stream.of("Remote Code Executor", classLoaderDescriptor, packageName)
			                           .filter(Objects::nonNull)
			                           .map(Object::toString)
			                           .map(String::trim)
			                           .filter(s -> !s.isEmpty())
			                           .collect(Collectors.joining(" - "));
			dialog.setTitle(title);
			dialog.initOwner(owner);
			dialog.initModality(Modality.NONE);
			dialog.setResizable(true);
			final Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
			final ChangeListener<RunningJvm> changeListener = (obs, old, newv) -> dialogWindow.hide();
			dialog.setOnHidden(e -> currentJvm.removeListener(changeListener));
			currentJvm.addListener(changeListener);
			dialogWindow.setOnCloseRequest(e -> dialog.hide());
			dialog.show();
		}
		catch (IOException e) {
			log.warn("Failed to initialize code executor", e);
		}
	}

}
