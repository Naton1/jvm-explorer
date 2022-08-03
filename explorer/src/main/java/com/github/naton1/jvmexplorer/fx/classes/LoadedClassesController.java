package com.github.naton1.jvmexplorer.fx.classes;

import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.JvmExplorer;
import com.github.naton1.jvmexplorer.agent.AgentException;
import com.github.naton1.jvmexplorer.agent.AgentPreparer;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.fx.TreeViewPlaceholderSkin;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ClassTreeHelper;
import com.github.naton1.jvmexplorer.helper.ExportHelper;
import com.github.naton1.jvmexplorer.helper.FilterHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.AgentConfiguration;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.UncheckedIOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

@Slf4j
public class LoadedClassesController {

	private static final NumberFormat numberFormat = NumberFormat.getInstance();

	private static final int CLASSES_NOT_LOADING = -1;

	private static final String AGENT_PATH = "agents/agent.jar";
	private static final File AGENT_LOG_FILE = new File(JvmExplorer.APP_DIR, "logs/agent.log");

	static {
		// Try to clean up on initial load. Prevents the file from growing too large.
		AGENT_LOG_FILE.delete();
	}

	private final ClassTreeHelper classTreeHelper = new ClassTreeHelper();
	private final AgentPreparer agentPreparer = new AgentPreparer();
	private final SimpleIntegerProperty loadedClassProgressCount = new SimpleIntegerProperty(CLASSES_NOT_LOADING);
	private final SimpleObjectProperty<ClassContent> currentClass = new SimpleObjectProperty<>();
	private final FilterHelper filterHelper = new FilterHelper();
	private final SimpleBooleanProperty agentLoading = new SimpleBooleanProperty();

	@FXML
	private TreeView<ClassTreeNode> classes;

	@FXML
	private TextField searchClasses;

	@FXML
	private TitledPane classesTitlePane;

	private FilterableTreeItem<ClassTreeNode> classesTreeRoot;
	private ScheduledExecutorService executorService;
	private ClientHandler clientHandler;
	private ExportHelper exportHelper;
	private AlertHelper alertHelper;
	private ObjectProperty<RunningJvm> currentJvm;
	private int serverPort;
	private JvmExplorerSettings settings;

	public ObjectProperty<ClassContent> currentClassProperty() {
		return currentClass;
	}

	public void initialize(Stage stage, ScheduledExecutorService executorService, ClientHandler clientHandler,
	                       ObjectProperty<RunningJvm> currentJvm, int serverPort, JvmExplorerSettings settings,
	                       FilterableTreeItem<ClassTreeNode> classesTreeRoot) {
		this.classesTreeRoot = classesTreeRoot;
		this.executorService = executorService;
		this.clientHandler = clientHandler;
		this.exportHelper = new ExportHelper(clientHandler);
		this.alertHelper = new AlertHelper(stage);
		this.currentJvm = currentJvm;
		this.serverPort = serverPort;
		this.settings = settings;
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
				// We are only searching classes here. A node will stay visible if any of its children are.
				if (t.getType() == ClassTreeNode.Type.CLASS) {
					return predicate.test(t.getLoadedClass().toString());
				}
				return false;
			};
		}, searchClasses.textProperty()));
		classes.setRoot(classesTreeRoot);
	}

	private void setupAgentLoader() {
		currentJvm.addListener((obs, old, newv) -> {
			if (newv == null) {
				loadedClassProgressCount.set(CLASSES_NOT_LOADING);
			}
			classesTreeRoot.getSourceChildren().clear();
			if (newv != null) {
				agentLoading.set(true);
				executorService.submit(() -> {
					final String agentArgs = buildAgentArgs(newv);
					try {
						final String localPath = agentPreparer.loadAgentOnFileSystem(AGENT_PATH);
						newv.loadAgent(localPath, agentArgs);
					}
					catch (AgentException | UncheckedIOException e) {
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
		                                            this::loadClasses,
		                                            settings));
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
		                                                   loadedClassProgressCount,
		                                                   classesTreeRoot.getChildren(),
		                                                   classesTreeRoot.getSourceChildren(),
		                                                   searchClasses.textProperty(),
		                                                   agentLoading));
		treeViewPlaceholderSkin.placeholderProperty().setValue(placeholderLabel);
		classes.setSkin(treeViewPlaceholderSkin);
	}

	private String buildAgentArgs(RunningJvm runningJvm) {
		return AgentConfiguration.builder()
		                         .hostName("localhost")
		                         .port(serverPort)
		                         .identifier(runningJvm.toIdentifier())
		                         .logLevel(Log.LEVEL_DEBUG)
		                         .logFilePath(AGENT_LOG_FILE.getAbsolutePath())
		                         .build()
		                         .toAgentArgs();
	}

	private void onSelectedClassChange(TreeItem<ClassTreeNode> old, TreeItem<ClassTreeNode> newv) {
		if (newv == null) {
			currentClass.setValue(null);
			return;
		}
		if (newv.getValue().getType() != ClassTreeNode.Type.CLASS) {
			return;
		}
		final LoadedClass loadedClass = newv.getValue().getLoadedClass();
		final RunningJvm selectedJvm = currentJvm.get();
		if (selectedJvm == null) {
			return;
		}
		log.debug("Selected class: {}", loadedClass);
		executorService.submit(() -> {
			final ClassContent classContent = clientHandler.getClassContent(selectedJvm, loadedClass);
			log.debug("Received class content for {}", loadedClass);
			if (classContent != null) {
				Platform.runLater(() -> currentClass.set(classContent));
			}
		});
	}

	public void loadClasses(RunningJvm runningJvm) {
		executorService.submit(() -> doLoadClasses(runningJvm));
	}

	private String getTitlePaneText() {
		final long visibleItems = classesTreeRoot.streamVisible().filter(p -> p.getLoadedClass() != null).count();
		final long sourceItems = classesTreeRoot.streamSource().filter(p -> p.getLoadedClass() != null).count();
		return "Loaded Classes (" + getLoadedClassDisplay(visibleItems, sourceItems) + ")";
	}

	private String getLoadedClassDisplay(long visibleItems, long sourceItems) {
		if (visibleItems == sourceItems) {
			return numberFormat.format(visibleItems);
		}
		return numberFormat.format(visibleItems) + "/" + numberFormat.format(sourceItems);
	}

	private String getPlaceholderText() {
		if (currentJvm.get() == null) {
			return "No JVM selected";
		}
		else if (loadedClassProgressCount.get() != CLASSES_NOT_LOADING) {
			if (loadedClassProgressCount.get() == 0) {
				return "Loading - Initializing";
			}
			return "Loading - " + loadedClassProgressCount.get() + " classes";
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

	private void doLoadClasses(RunningJvm runningJvm) {
		Platform.runLater(() -> loadedClassProgressCount.set(0));
		final List<LoadedClass> loadedClasses = clientHandler.getLoadedClasses(runningJvm, loadedClassPercent -> {
			Platform.runLater(() -> this.loadedClassProgressCount.set(loadedClassPercent));
		});
		if (loadedClasses == null) {
			log.warn("Failed to load active classes: {}", runningJvm);
			return;
		}
		log.debug("Received loaded classes for {}", runningJvm);
		final ClassTreeNode classTreeRoot = buildClassTree(loadedClasses);
		Platform.runLater(() -> {
			final FilterableTreeItem<ClassTreeNode> root = classTreeRoot.toTreeItem();
			classesTreeRoot.getSourceChildren().setAll(root.getSourceChildren());
			loadedClassProgressCount.set(CLASSES_NOT_LOADING);
		});
	}

	private ClassTreeNode buildClassTree(List<LoadedClass> loadedClasses) {
		if (this.settings.getShowClassLoader().get()) {
			return classTreeHelper.buildClassLoaderTree(loadedClasses);
		}
		else {
			return classTreeHelper.buildClassTree(loadedClasses);
		}
	}

}
