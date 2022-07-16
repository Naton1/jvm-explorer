package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ExportHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ActiveClass;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
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

@RequiredArgsConstructor
@Slf4j
public class ClassCellFactory implements Callback<TreeView<PackageTreeNode>, TreeCell<PackageTreeNode>> {

	private final ExecutorService executorService;
	private final AlertHelper alertHelper;
	private final ObjectProperty<RunningJvm> currentJvm;
	private final ClientHandler clientHandler;
	private final FilterableTreeItem<PackageTreeNode> classesTreeRoot;
	private final ExportHelper exportHelper;
	private final BooleanBinding jvmLoaded;
	private final Consumer<RunningJvm> onLoadActiveClasses;

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
			return String.valueOf(packageTreeNode.getActiveClass());
		}, treeCell.itemProperty()));
		treeCell.tooltipProperty().bind(Bindings.when(Bindings.createBooleanBinding(() -> {
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return false;
			}
			return packageTreeNode.getActiveClass() != null;
		}, treeCell.itemProperty())).then(tooltip).otherwise((Tooltip) null));
	}

	private void setupContextMenu(TreeCell<PackageTreeNode> treeCell, TreeView<PackageTreeNode> classes) {
		final ContextMenu classesContextMenu = new ContextMenu();
		final MenuItem exportClass = new MenuItem("Export Class");
		exportClass.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			final PackageTreeNode packageTreeNode = treeCell.getItem();
			if (packageTreeNode == null) {
				return;
			}
			final ActiveClass activeClass = packageTreeNode.getActiveClass();
			if (activeClass == null) {
				return;
			}
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Export Class");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Class Files", "*.class"));
			fileChooser.setInitialFileName(activeClass.getSimpleName() + ".class");
			final File selectedFile = fileChooser.showSaveDialog(classes.getScene().getWindow());
			if (selectedFile == null) {
				return;
			}
			executorService.submit(() -> export(selectedFile, activeClass, activeJvm));
		});
		final MenuItem exportClasses = new MenuItem("Export Classes");
		exportClasses.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Export Classes");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JAR Files", "*.jar"));
			fileChooser.setInitialFileName(activeJvm.getName());
			final File selectedFile = fileChooser.showSaveDialog(classes.getScene().getWindow());
			if (selectedFile == null) {
				return;
			}
			final List<String> roots = classesTreeRoot.streamVisible()
			                                          .map(PackageTreeNode::getActiveClass)
			                                          .filter(Objects::nonNull)
			                                          .map(ActiveClass::getName)
			                                          .collect(Collectors.toList());
			executorService.submit(() -> export(selectedFile, roots, activeJvm));
		});
		final MenuItem reloadClasses = new MenuItem("Refresh Classes");
		reloadClasses.setOnAction(e -> {
			final RunningJvm activeJvm = currentJvm.get();
			if (activeJvm == null) {
				return;
			}
			classesTreeRoot.getSourceChildren().clear();
			// I think my java version is broken... it can't compile obvious things like this without casting
			executorService.submit((Runnable) () -> onLoadActiveClasses.accept(activeJvm));
		});

		treeCell.setContextMenu(classesContextMenu);

		reloadClasses.disableProperty().bind(jvmLoaded.not());
		exportClasses.disableProperty().bind(jvmLoaded.not());

		final MenuItem replaceClass = new MenuItem("Replace Class");
		replaceClass.setOnAction(e -> {
			final PackageTreeNode selectedClass = treeCell.getItem();
			if (selectedClass == null || selectedClass.getActiveClass() == null) {
				return;
			}
			final RunningJvm jvm = currentJvm.get();
			if (jvm == null) {
				return;
			}
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Replace Class");
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Class Files", "*.class"));
			final File selectedFile = fileChooser.showOpenDialog(classes.getScene().getWindow());
			if (selectedFile == null) {
				return;
			}
			executorService.submit(() -> {
				replaceClass(selectedFile, selectedClass.getActiveClass(), jvm);
			});
		});

		classesContextMenu.getItems().addAll(exportClasses, reloadClasses);
		treeCell.itemProperty().addListener((obs, old, newv) -> {
			classesContextMenu.getItems().clear();
			if (newv != null && newv.getActiveClass() != null) {
				classesContextMenu.getItems().addAll(exportClass, replaceClass, new SeparatorMenuItem());
			}
			classesContextMenu.getItems().addAll(exportClasses, reloadClasses);
		});

		treeCell.setContextMenu(classesContextMenu);
	}

	private void export(File selectedFile, ActiveClass activeClass, RunningJvm activeJvm) {
		try {
			final byte[] classContent = clientHandler.getExportFile(activeJvm, activeClass.getName());
			Files.write(selectedFile.toPath(), classContent);
		}
		catch (IOException ex) {
			log.warn("Failed to write class file {}", activeClass.getName(), ex);
		}
	}

	private void export(File selectedFile, List<String> classes, RunningJvm activeJvm) {
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
					return "Creating jar: " + progress.get() + " classes";
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

	private void replaceClass(File selectedFile, ActiveClass activeClass, RunningJvm activeJvm) {
		final byte[] contents;
		try {
			contents = Files.readAllBytes(selectedFile.toPath());
		}
		catch (IOException ex) {
			log.warn("Failed to read file", ex);
			return;
		}
		final boolean replaced = clientHandler.replaceClass(activeJvm, activeClass.getName(), contents);
		Platform.runLater(() -> {
			final Alert.AlertType alertType = replaced ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
			final String title = replaced ? "Replaced Class" : "Replace Failed";
			final String header = replaced ? "Successfully replaced class" : "Class replacement failed";
			alertHelper.show(alertType, title, header);
		});
	}

}
