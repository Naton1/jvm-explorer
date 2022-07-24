package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ExportHelper;
import com.github.naton1.jvmexplorer.helper.PatchHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class ClassCellFactory implements Callback<TreeView<PackageTreeNode>, TreeCell<PackageTreeNode>> {

	private final PatchHelper patchHelper = new PatchHelper();

	private final ExecutorService executorService;
	private final AlertHelper alertHelper;
	private final ObjectProperty<RunningJvm> currentJvm;
	private final ClientHandler clientHandler;
	private final FilterableTreeItem<PackageTreeNode> classesTreeRoot;
	private final ExportHelper exportHelper;
	private final BooleanBinding jvmLoaded;
	private final Consumer<RunningJvm> onLoadClasses;
	private final JvmExplorerSettings settings;

	@Override
	public TreeCell<PackageTreeNode> call(TreeView<PackageTreeNode> classes) {
		final TreeCell<PackageTreeNode> treeCell = new TreeCell<>();
		setupImageBinding(treeCell);
		setupTextBinding(treeCell);
		setupTooltipBinding(treeCell);
		setupContextMenu(treeCell, classes);
		return treeCell;
	}

	private void setupImageBinding(TreeCell<PackageTreeNode> treeCell) {
		treeCell.graphicProperty().bind(Bindings.createObjectBinding(() -> {
			final PackageTreeNode item = treeCell.getItem();
			if (item == null) {
				return null;
			}
			return new ImageView(item.getType().getImage());
		}, treeCell.itemProperty()));
	}

	private void setupTextBinding(TreeCell<PackageTreeNode> treeCell) {
		treeCell.textProperty()
		        .bind(Bindings.when(treeCell.itemProperty().isNotNull())
		                      .then(treeCell.itemProperty().asString())
		                      .otherwise(""));
	}

	private void setupTooltipBinding(TreeCell<PackageTreeNode> treeCell) {
		final Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(Bindings.createStringBinding(() -> {
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return "";
			}
			switch (packageTreeNode.getType()) {
			case CLASSLOADER:
				return packageTreeNode.getClassLoaderDescriptor().getDescription();
			case PACKAGE:
				return packageTreeNode.getPackagePart();
			case CLASS:
				return packageTreeNode.getLoadedClass().toString();
			default:
				log.warn("Unknown type: {}", packageTreeNode.getType());
				return "";
			}
		}, treeCell.itemProperty()));
		treeCell.tooltipProperty()
		        .bind(Bindings.when(treeCell.itemProperty().isNotNull()).then(tooltip).otherwise((Tooltip) null));
	}

	private void setupContextMenu(TreeCell<PackageTreeNode> treeCell, TreeView<PackageTreeNode> classes) {
		final ContextMenu classesContextMenu = new ContextMenu();

		final MenuItem scopedExport = createScopedExport(treeCell, classes);
		final MenuItem exportClasses = createExportClasses(classes);
		final MenuItem reloadClasses = createReloadClasses();
		final MenuItem scopedReplace = createScopedReplace(treeCell, classes);
		final MenuItem replaceClasses = createReplaceClasses(classes);
		final MenuItem includeClassLoader = createShowClassLoader(reloadClasses);

		treeCell.itemProperty().addListener((obs, old, newv) -> {
			classesContextMenu.getItems().clear();
			if (newv != null) {
				classesContextMenu.getItems().addAll(scopedExport, scopedReplace, new SeparatorMenuItem());
			}
			classesContextMenu.getItems()
			                  .addAll(exportClasses,
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

	private MenuItem createReplaceClasses(TreeView<PackageTreeNode> classes) {
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

	private MenuItem createScopedReplace(TreeCell<PackageTreeNode> treeCell, TreeView<PackageTreeNode> classes) {
		final MenuItem scopedReplace = new MenuItem("Replace Class");
		scopedReplace.textProperty().bind(Bindings.createStringBinding(() -> {
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return "";
			}
			switch (packageTreeNode.getType()) {
			case CLASSLOADER:
				return "Replace Class Loader";
			case PACKAGE:
				return "Replace Package";
			case CLASS:
				return "Replace Class";
			}
			log.warn("Unknown type: {}", packageTreeNode.getType());
			return "";
		}, treeCell.itemProperty()));
		scopedReplace.setOnAction(e -> {
			final RunningJvm jvm = currentJvm.get();
			if (jvm == null) {
				return;
			}
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return;
			}
			switch (packageTreeNode.getType()) {
			case CLASSLOADER:
				final File importClassLoader = selectImportJarFile(classes.getScene().getWindow());
				if (importClassLoader == null) {
					return;
				}
				final ClassLoaderDescriptor classLoaderDescriptor = packageTreeNode.getClassLoaderDescriptor();
				executorService.submit(() -> replaceClasses(importClassLoader, jvm, classLoaderDescriptor));
				break;
			case PACKAGE:
				final File importPackage = selectImportJarFile(classes.getScene().getWindow());
				if (importPackage == null) {
					return;
				}
				final ClassLoaderDescriptor packageClassLoader = getPackageClassLoader(treeCell.getTreeItem());
				executorService.submit(() -> replaceClasses(importPackage, jvm, packageClassLoader));
				break;
			case CLASS:
				final File selectedFile = selectImportClassFile(classes.getScene().getWindow());
				if (selectedFile == null) {
					return;
				}
				executorService.submit(() -> replaceClass(selectedFile, packageTreeNode.getLoadedClass(), jvm));
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

	private MenuItem createExportClasses(TreeView<PackageTreeNode> classes) {
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
			                                                       .map(PackageTreeNode::getLoadedClass)
			                                                       .filter(Objects::nonNull)
			                                                       .collect(Collectors.toList());
			executorService.submit(() -> export(selectedFile, loadedClasses, activeJvm));
		});
		return exportClasses;
	}

	private MenuItem createScopedExport(TreeCell<PackageTreeNode> treeCell, TreeView<PackageTreeNode> classes) {
		final MenuItem scopedExport = new MenuItem();
		scopedExport.textProperty().bind(Bindings.createStringBinding(() -> {
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return "";
			}
			switch (packageTreeNode.getType()) {
			case CLASSLOADER:
				return "Export Class Loader";
			case PACKAGE:
				return "Export Package";
			case CLASS:
				return "Export Class";
			}
			log.warn("Unknown type: {}", packageTreeNode.getType());
			return "";
		}, treeCell.itemProperty()));
		scopedExport.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return;
			}
			switch (packageTreeNode.getType()) {
			case CLASSLOADER:
				final File exportClassLoader = selectExportJarFile(packageTreeNode.getPackagePart(),
				                                                   classes.getScene().getWindow());
				if (exportClassLoader == null) {
					return;
				}
				final ClassLoaderDescriptor classLoaderDescriptor = packageTreeNode.getClassLoaderDescriptor();
				log.debug("Exporting class loader: {}", classLoaderDescriptor);
				final List<LoadedClass> classesInClassLoader = getClassesInPackage("", classLoaderDescriptor);
				executorService.submit(() -> export(exportClassLoader, classesInClassLoader, activeJvm));
				break;
			case PACKAGE:
				final File exportPackage = selectExportJarFile(packageTreeNode.getPackagePart(),
				                                               classes.getScene().getWindow());
				if (exportPackage == null) {
					return;
				}
				final String fullPackageName = getPackageName(treeCell.getTreeItem());
				final ClassLoaderDescriptor packageClassLoader =
						this.settings.getShowClassLoader().get() ? getPackageClassLoader(treeCell.getTreeItem()) :
						null;
				log.debug("Exporting package: {} in classloader: {}", fullPackageName, packageClassLoader);
				final List<LoadedClass> classesInPackage = getClassesInPackage(fullPackageName, packageClassLoader);
				executorService.submit(() -> export(exportPackage, classesInPackage, activeJvm));
				break;
			case CLASS:
				final LoadedClass loadedClass = packageTreeNode.getLoadedClass();
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

	private File selectImportJarFile(Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Replace Classes");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
		return fileChooser.showOpenDialog(owner);
	}

	private File selectImportClassFile(Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Replace Class");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Class Files", "*.class"));
		return fileChooser.showOpenDialog(owner);
	}

	private String getPackageName(TreeItem<PackageTreeNode> packageNode) {
		final List<String> packageParts = Stream.iterate(packageNode,
		                                                 o -> o != null && o.getValue() != null
		                                                      && o.getValue().getType() == PackageTreeNode.Type.PACKAGE,
		                                                 TreeItem::getParent)
		                                        .map(item -> item.getValue().getPackagePart())
		                                        .collect(Collectors.toCollection(ArrayList::new));
		Collections.reverse(packageParts);
		return String.join(".", packageParts);
	}

	private List<LoadedClass> getClassesInPackage(String fullPackageName, ClassLoaderDescriptor packageClassLoader) {
		return classesTreeRoot.streamVisible()
		                      .filter(p -> p.getType() == PackageTreeNode.Type.CLASS)
		                      .map(PackageTreeNode::getLoadedClass)
		                      .filter(c -> c.getName().startsWith(fullPackageName))
		                      .filter(c -> (packageClassLoader == null)
		                                   || packageClassLoader.equals(c.getClassLoaderDescriptor()))
		                      .collect(Collectors.toList());
	}

	private ClassLoaderDescriptor getPackageClassLoader(TreeItem<PackageTreeNode> packageNode) {
		return Stream.iterate(packageNode, o -> o != null && o.getValue() != null, TreeItem::getParent)
		             .map(TreeItem::getValue)
		             .filter(p -> p.getType() == PackageTreeNode.Type.CLASSLOADER)
		             .findFirst()
		             .map(PackageTreeNode::getClassLoaderDescriptor)
		             .orElse(null);
	}

	private File selectExportClassFile(String initialFileName, Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Class");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Class Files", "*.class"));
		fileChooser.setInitialFileName(initialFileName);
		return fileChooser.showSaveDialog(owner);
	}

	private File selectExportJarFile(String initialFileName, Window owner) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export Classes");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
		fileChooser.setInitialFileName(initialFileName);
		return fileChooser.showSaveDialog(owner);
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

}
