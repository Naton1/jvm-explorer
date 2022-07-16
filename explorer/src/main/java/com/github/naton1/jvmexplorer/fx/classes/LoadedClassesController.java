package com.github.naton1.jvmexplorer.fx.classes;

import com.github.naton1.jvmexplorer.agent.AgentException;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ExportHelper;
import com.github.naton1.jvmexplorer.helper.FilterHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ActiveClass;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;

@Slf4j
public class LoadedClassesController {

	private static final int CLASSES_NOT_LOADING = -1;

	private final FilterableTreeItem<PackageTreeNode> classesTreeRoot = new FilterableTreeItem<>();
	private final SimpleIntegerProperty loadedClassCount = new SimpleIntegerProperty(CLASSES_NOT_LOADING);
	private final SimpleObjectProperty<ClassContent> currentClass = new SimpleObjectProperty<>();
	private final FilterHelper filterHelper = new FilterHelper();
	private final SimpleBooleanProperty agentLoading = new SimpleBooleanProperty();

	@FXML
	private TreeView<PackageTreeNode> classes;

	@FXML
	private TextField searchClasses;

	@FXML
	private TitledPane classesTitlePane;

	private ExecutorService executorService;
	private ClientHandler clientHandler;
	private ExportHelper exportHelper;
	private AlertHelper alertHelper;
	private ObjectProperty<RunningJvm> currentJvm;
	private BooleanBinding jvmLoaded;

	public ObjectProperty<ClassContent> currentClassProperty() {
		return currentClass;
	}

	public void initialize(Stage stage, ExecutorService executorService, ClientHandler clientHandler,
	                       ObjectProperty<RunningJvm> currentJvm) {
		this.executorService = executorService;
		this.clientHandler = clientHandler;
		this.exportHelper = new ExportHelper(clientHandler);
		this.alertHelper = new AlertHelper(stage);
		this.currentJvm = currentJvm;
		this.jvmLoaded = currentJvm.isNotNull().and(loadedClassCount.isEqualTo(CLASSES_NOT_LOADING));
		initialize();
	}

	private void initialize() {
		setupTreeSearching();
		setupAgentLoader();
		setupClassesCore();
		setupTitlePaneText();
		setupTreePlaceholder();
	}

	private void setupTreeSearching() {
		classesTreeRoot.predicateProperty().bind(Bindings.createObjectBinding(() -> {
			final String text = searchClasses.getText().trim();
			final Predicate<String> predicate = filterHelper.createStringPredicate(text);
			return t -> {
				if (t.getActiveClass() != null) {
					return predicate.test(t.getActiveClass().toString());
				}
				return predicate.test(t.toString());
			};
		}, searchClasses.textProperty()));
		classes.setRoot(classesTreeRoot);
	}

	private void setupAgentLoader() {
		currentJvm.addListener((obs, old, newv) -> {
			if (newv == null) {
				loadedClassCount.set(CLASSES_NOT_LOADING);
			}
			classesTreeRoot.getSourceChildren().clear();
			if (newv != null) {
				agentLoading.set(true);
				executorService.submit(() -> {
					try {
						newv.loadAgent();
					}
					catch (AgentException e) {
						Platform.runLater(() -> {
							alertHelper.showError("Agent Error", "Failed to load agent in JVM", e);
							currentJvm.set(null);
						});
					}
					finally {
						Platform.runLater(() -> agentLoading.set(false));
					}
				});
			}
			if (old != null) {
				executorService.submit(() -> clientHandler.close(old));
			}
		});
	}

	private void setupClassesCore() {
		classes.getSelectionModel()
		       .selectedItemProperty()
		       .addListener((obs, old, newv) -> onSelectedClassChange(old, newv));
		classes.setShowRoot(false);

		classes.setCellFactory(new ClassCellFactory(executorService,
		                                            alertHelper,
		                                            currentJvm,
		                                            clientHandler,
		                                            classesTreeRoot,
		                                            exportHelper,
		                                            jvmLoaded,
		                                            this::loadActiveClasses));
	}

	private void setupTitlePaneText() {
		classesTitlePane.textProperty()
		                .bind(Bindings.createStringBinding(this::getTitlePaneText,
		                                                   currentJvm,
		                                                   classesTreeRoot.getChildren(),
		                                                   classesTreeRoot.getSourceChildren(),
		                                                   searchClasses.textProperty()));
	}

	private void setupTreePlaceholder() {
		final TreeViewPlaceholderSkin<?> treeViewPlaceholderSkin = new TreeViewPlaceholderSkin<>(classes);
		final Label placeholderLabel = new Label();
		placeholderLabel.textProperty()
		                .bind(Bindings.createStringBinding(this::getPlaceholderText,
		                                                   currentJvm,
		                                                   loadedClassCount,
		                                                   classesTreeRoot.getChildren(),
		                                                   classesTreeRoot.getSourceChildren(),
		                                                   searchClasses.textProperty(),
		                                                   agentLoading));
		treeViewPlaceholderSkin.placeholderProperty().setValue(placeholderLabel);
		classes.setSkin(treeViewPlaceholderSkin);
	}

	private void onSelectedClassChange(TreeItem<PackageTreeNode> old, TreeItem<PackageTreeNode> newv) {
		if (newv == null) {
			currentClass.setValue(null);
			return;
		}
		if (newv.getValue().getActiveClass() == null) {
			// Selected a package, not a class
			return;
		}
		final RunningJvm selectedJvm = currentJvm.get();
		if (selectedJvm == null) {
			return;
		}
		log.debug("Received class content");
		executorService.submit(() -> {
			final ClassContent classContent = clientHandler.getClassContent(selectedJvm,
			                                                                newv.getValue().getActiveClass());
			if (classContent != null) {
				Platform.runLater(() -> {
					currentClass.set(classContent);
				});
			}
		});
	}

	public void loadActiveClasses(RunningJvm runningJvm) {
		executorService.submit(() -> doLoadActiveClasses(runningJvm));
	}

	private String getTitlePaneText() {
		final long visibleItems = classesTreeRoot.streamVisible().filter(p -> p.getActiveClass() != null).count();
		final long sourceItems = classesTreeRoot.streamSource().filter(p -> p.getActiveClass() != null).count();
		return "Loaded Classes (" + (visibleItems == sourceItems ? visibleItems : (visibleItems + "/" + sourceItems))
		       + ")";
	}

	private String getPlaceholderText() {
		if (currentJvm.get() == null) {
			return "No JVM selected";
		}
		else if (loadedClassCount.get() != CLASSES_NOT_LOADING) {
			if (loadedClassCount.get() == 0) {
				return "Loading - Initializing";
			}
			return "Loading - " + loadedClassCount.get() + " classes";
		}
		else if (agentLoading.get()) {
			return "Agent attaching to process";
		}
		else if (!searchClasses.getText().trim().isEmpty()) {
			return "No classes found";
		}
		else {
			return "No classes... possible " + "agent error";
		}
	}

	private void doLoadActiveClasses(RunningJvm runningJvm) {
		Platform.runLater(() -> loadedClassCount.set(0));
		final List<ActiveClass> activeClasses = clientHandler.getActiveClasses(runningJvm, loadedClassPercent -> {
			Platform.runLater(() -> this.loadedClassCount.set(loadedClassPercent));
		});
		if (activeClasses == null) {
			log.warn("Failed to load active classes: {}", runningJvm);
			return;
		}
		log.debug("Received loaded classes for {}", runningJvm);
		final PackageTreeNode packageTreeRoot = buildPackageTree(activeClasses);
		Platform.runLater(() -> {
			final FilterableTreeItem<PackageTreeNode> root = packageTreeRoot.toTreeItem();
			classesTreeRoot.getSourceChildren().setAll(root.getSourceChildren());
			loadedClassCount.set(CLASSES_NOT_LOADING);
		});
	}

	private PackageTreeNode buildPackageTree(List<ActiveClass> activeClasses) {
		final PackageTreeNode packageTreeRoot = new PackageTreeNode(null, null);
		for (ActiveClass activeClass : activeClasses) {
			final String[] classNameParts = activeClass.getName().split("\\.");
			PackageTreeNode packageTree = packageTreeRoot;
			for (int i = 0; i < classNameParts.length - 1; i++) {
				final String classNamePart = classNameParts[i];
				packageTree = packageTree.getChildren()
				                         .computeIfAbsent(classNamePart, k -> new PackageTreeNode(null,
				                                                                                  classNamePart));
			}
			final String simpleClassName = classNameParts[classNameParts.length - 1];
			final PackageTreeNode previous = packageTree.getChildren()
			                                            .put(simpleClassName,
			                                                 new PackageTreeNode(activeClass, simpleClassName));
			if (previous != null && previous.getActiveClass() != null) {
				log.warn("Loaded duplicate class: {}", simpleClassName);
			}
		}
		return packageTreeRoot;
	}

}
