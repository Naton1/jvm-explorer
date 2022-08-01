package com.github.naton1.jvmexplorer;

import com.esotericsoftware.kryonet.Connection;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.fx.classes.LoadedClassesController;
import com.github.naton1.jvmexplorer.fx.jvms.RunningJvmsController;
import com.github.naton1.jvmexplorer.fx.openclass.CurrentClassController;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.net.JvmExplorerServer;
import com.github.naton1.jvmexplorer.net.OpenPortProvider;
import com.github.naton1.jvmexplorer.net.ServerLauncher;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class JvmExplorerController {

	private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
	@FXML
	private RunningJvmsController runningJvmsController;
	@FXML
	private LoadedClassesController loadedClassesController;
	@FXML
	private CurrentClassController currentClassController;
	private Stage stage;
	private AlertHelper alertHelper;
	private final ClientHandler clientHandler = ClientHandler.builder()
	                                                         .onConnect(this::onConnect)
	                                                         .onDisconnect(this::onDisconnect)
	                                                         .build();
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
		final int serverPort = server.getPort();

		this.runningJvmsController.initialize(stage, executorService);
		this.loadedClassesController.initialize(stage,
		                                        executorService,
		                                        clientHandler,
		                                        currentJvm,
		                                        serverPort,
		                                        jvmExplorerSettings);
		this.currentClassController.initialize(stage, executorService, clientHandler, currentJvm, currentClass);
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
