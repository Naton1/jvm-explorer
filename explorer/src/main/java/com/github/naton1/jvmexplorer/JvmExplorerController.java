package com.github.naton1.jvmexplorer;

import com.esotericsoftware.kryonet.Connection;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.fx.classes.ClassTreeNode;
import com.github.naton1.jvmexplorer.fx.classes.FilterableTreeItem;
import com.github.naton1.jvmexplorer.fx.classes.LoadedClassesController;
import com.github.naton1.jvmexplorer.fx.jvms.RunningJvmsController;
import com.github.naton1.jvmexplorer.fx.openclass.CurrentClassController;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.ScreenHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.net.JvmExplorerServer;
import com.github.naton1.jvmexplorer.net.OpenPortProvider;
import com.github.naton1.jvmexplorer.net.ServerLauncher;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.helper.VerboseScheduledExecutorService;
import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class JvmExplorerController {

	private final ScheduledExecutorService executorService =
			new VerboseScheduledExecutorService(Executors.newScheduledThreadPool(
			8));

	private final ClientHandler clientHandler = ClientHandler.builder()
	                                                         .onConnect(this::onConnect)
	                                                         .onDisconnect(this::onDisconnect)
	                                                         .build();

	@FXML
	private RunningJvmsController runningJvmsController;
	@FXML
	private LoadedClassesController loadedClassesController;
	@FXML
	private CurrentClassController currentClassController;

	@FXML
	private SplitPane splitPane;

	private Stage stage;
	private AlertHelper alertHelper;
	private JvmExplorerServer server;

	public void initialize(Stage stage) {
		this.stage = stage;
		this.alertHelper = new AlertHelper(stage);
		final OpenPortProvider openPortProvider = new OpenPortProvider();
		final ServerLauncher serverLauncher = new ServerLauncher(openPortProvider);
		server = serverLauncher.launch(executorService, clientHandler);
		stage.setOnHidden(e -> {
			log.debug("Stage hidden, cleaning up resources");
			try {
				server.dispose();
			}
			catch (IOException ex) {
				log.warn("Failed to close server", ex);
			}
			executorService.shutdown();
		});

		setupTitlePaneText();

		wireChildControllers();
	}

	private void setupTitlePaneText() {
		stage.titleProperty().bind(Bindings.createStringBinding(() -> {
			final RunningJvm currentJvm = this.runningJvmsController.getCurrentJvm();
			if (currentJvm != null) {
				return "JVM Explorer - " + currentJvm;
			}
			return "JVM Explorer";
		}, this.runningJvmsController.currentJvmProperty()));
	}

	private void wireChildControllers() {
		final ObjectProperty<RunningJvm> currentJvm = this.runningJvmsController.currentJvmProperty();
		final ObjectProperty<ClassContent> currentClass = this.loadedClassesController.currentClassProperty();
		final JvmExplorerSettings jvmExplorerSettings =
				JvmExplorerSettings.load(JvmExplorerSettings.DEFAULT_SETTINGS_FILE);
		jvmExplorerSettings.configureAutoSaving(JvmExplorerSettings.DEFAULT_SETTINGS_FILE);

		initializeStage(jvmExplorerSettings);

		final int serverPort = server.getPort();

		final FilterableTreeItem<ClassTreeNode> classesTreeRoot = new FilterableTreeItem<>();

		this.runningJvmsController.initialize(stage, executorService);
		this.loadedClassesController.initialize(stage,
		                                        executorService,
		                                        clientHandler,
		                                        currentJvm,
		                                        serverPort,
		                                        jvmExplorerSettings,
		                                        classesTreeRoot);
		this.currentClassController.initialize(stage,
		                                       executorService,
		                                       clientHandler,
		                                       currentJvm,
		                                       currentClass,
		                                       classesTreeRoot,
		                                       loadedClassesController::select);
	}

	private void initializeStage(JvmExplorerSettings jvmExplorerSettings) {
		final EventHandler<WindowEvent> onFirstShow = e -> {

			if (Double.isNaN(jvmExplorerSettings.getWidth().get())) {
				jvmExplorerSettings.getWidth().set(this.stage.getWidth());
			}
			if (Double.isNaN(jvmExplorerSettings.getHeight().get())) {
				jvmExplorerSettings.getHeight().set(this.stage.getHeight());
			}

			if (jvmExplorerSettings.getWidth().get() < 200) {
				jvmExplorerSettings.getWidth().set(1200);
			}
			if (jvmExplorerSettings.getHeight().get() < 200) {
				jvmExplorerSettings.getHeight().set(600);
			}

			this.stage.setWidth(jvmExplorerSettings.getWidth().get());
			this.stage.setHeight(jvmExplorerSettings.getHeight().get());

			this.stage.setMaximized(jvmExplorerSettings.getMaximized().get());

			this.stage.maximizedProperty().addListener((obs, old, newv) -> {
				jvmExplorerSettings.getMaximized().set(newv);
			});

			jvmExplorerSettings.getWidth().addListener((obs, old, newv) -> {
				this.stage.setWidth(newv.doubleValue());
			});

			jvmExplorerSettings.getHeight().addListener((obs, old, newv) -> {
				this.stage.setHeight(newv.doubleValue());
			});

			jvmExplorerSettings.getHeight().bind(this.stage.heightProperty());
			jvmExplorerSettings.getWidth().bind(this.stage.widthProperty());

			if (Double.isNaN(jvmExplorerSettings.getX().get())) {
				jvmExplorerSettings.getX().set(this.stage.getX());
			}
			if (Double.isNaN(jvmExplorerSettings.getY().get())) {
				jvmExplorerSettings.getY().set(this.stage.getY());
			}

			final Rectangle base = new Rectangle((int) jvmExplorerSettings.getX().get(),
			                                     (int) jvmExplorerSettings.getY().get(),
			                                     (int) jvmExplorerSettings.getWidth().get(),
			                                     (int) jvmExplorerSettings.getHeight().get());

			if (!ScreenHelper.isOnScreen(base, 0.20)) {
				this.stage.centerOnScreen();
			}
			else {
				this.stage.setX(jvmExplorerSettings.getX().get());
				this.stage.setY(jvmExplorerSettings.getY().get());
			}

			jvmExplorerSettings.getX().addListener((obs, old, newv) -> {
				this.stage.setX(newv.doubleValue());
			});

			jvmExplorerSettings.getY().addListener((obs, old, newv) -> {
				this.stage.setY(newv.doubleValue());
			});

			jvmExplorerSettings.getX().bind(this.stage.xProperty());
			jvmExplorerSettings.getY().bind(this.stage.yProperty());

			final SplitPane.Divider firstDivider = splitPane.getDividers().get(0);
			final SplitPane.Divider secondDivider = splitPane.getDividers().get(1);

			// Let's set it a first time to try and prevent graphical issues
			firstDivider.positionProperty().set(jvmExplorerSettings.getFirstDividerPosition().get());
			secondDivider.positionProperty().set(jvmExplorerSettings.getSecondDividerPosition().get());

			Platform.runLater(() -> {
				// Divider positions not respected if scene size != stage on initial show
				// Therefor we have to run this later after the scene shows the first time
				// https://stackoverflow.com/questions/15041332/javafx-splitpane-divider-position-inconsistent-behaviour
				firstDivider.positionProperty().bindBidirectional(jvmExplorerSettings.getFirstDividerPosition());
				secondDivider.positionProperty().bindBidirectional(jvmExplorerSettings.getSecondDividerPosition());
			});
		};

		this.stage.addEventHandler(WindowEvent.WINDOW_SHOWN, onFirstShow);
		this.stage.addEventHandler(WindowEvent.WINDOW_SHOWN, e -> {
			this.stage.removeEventHandler(WindowEvent.WINDOW_SHOWN, onFirstShow);
		});
	}

	private void onConnect(RunningJvm jvm, Connection connection) {
		log.debug("Connected to {}", jvm);
		if (jvm.equals(runningJvmsController.getCurrentJvm())) {
			loadedClassesController.loadClasses(jvm);
		}
	}

	private void onDisconnect(RunningJvm jvm) {
		Platform.runLater(() -> {
			final RunningJvm selectedJvm = runningJvmsController.getCurrentJvm();
			if (jvm.equals(selectedJvm)) {
				if (!this.stage.isShowing()) {
					return;
				}
				alertHelper.showError("Connection Lost", "Connection lost to JVM");
				runningJvmsController.setCurrentJvm(null);
			}
		});
	}

}
