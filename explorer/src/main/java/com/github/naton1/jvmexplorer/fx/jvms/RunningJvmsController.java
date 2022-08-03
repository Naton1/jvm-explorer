package com.github.naton1.jvmexplorer.fx.jvms;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.agent.RunningJvmLoader;
import com.github.naton1.jvmexplorer.helper.AlertHelper;
import com.github.naton1.jvmexplorer.helper.FilterHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class RunningJvmsController {

	private static final NumberFormat numberFormat = NumberFormat.getInstance();

	private final SimpleObjectProperty<RunningJvm> currentJvm = new SimpleObjectProperty<>();
	private final ObservableList<RunningJvm> runningJvms = FXCollections.observableArrayList();
	private final RunningJvmLoader runningJvmLoader = new RunningJvmLoader();
	private final FilterHelper filterHelper = new FilterHelper();

	@FXML
	private ListView<RunningJvm> processes;

	@FXML
	private TextField searchJvms;

	@FXML
	private TitledPane jvmsTitlePane;

	private ScheduledExecutorService executorService;

	public RunningJvm getCurrentJvm() {
		return currentJvm.get();
	}

	public void setCurrentJvm(RunningJvm runningJvm) {
		currentJvm.set(runningJvm);
	}

	public ObjectProperty<RunningJvm> currentJvmProperty() {
		return currentJvm;
	}

	public Node getRoot() {
		return jvmsTitlePane;
	}

	public void initialize(Stage stage, ScheduledExecutorService scheduledExecutorService) {
		final AlertHelper alertHelper = new AlertHelper(stage);
		this.executorService = scheduledExecutorService;
		processes.setPlaceholder(new Label("No JVMs found"));
		bindSelectedItemToJvmProperty();
		setupJvmSearching();
		setupTitlePaneText(); // run this after setting up searching, it depends on items being set
		processes.setCellFactory(new RunningJvmListCellFactory(executorService, alertHelper, currentJvm));
		scheduleRunningJvmUpdater();
	}

	private void bindSelectedItemToJvmProperty() {
		// Creates a two-way directional binding
		this.processes.getSelectionModel().selectedItemProperty().addListener((obs, old, newv) -> {
			if (newv != currentJvm.get()) {
				currentJvm.set(newv);
			}
		});
		this.currentJvm.addListener((obs, old, newv) -> {
			if (newv == null) {
				this.processes.getSelectionModel().clearSelection();
			}
			else {
				this.processes.getSelectionModel().select(newv);
			}
		});
	}

	private void setupJvmSearching() {
		final FilteredList<RunningJvm> searchedJvms = new FilteredList<>(runningJvms);
		searchedJvms.predicateProperty().bind(Bindings.createObjectBinding(() -> {
			final String text = searchJvms.getText().trim();
			final Predicate<String> predicate = filterHelper.createStringPredicate(text);
			return t -> predicate.test(t.toString());
		}, searchJvms.textProperty()));
		// Set items before binding jvm title pane text property
		processes.setItems(searchedJvms);
	}

	private void setupTitlePaneText() {
		jvmsTitlePane.textProperty().bind(Bindings.createStringBinding(() -> {
			final int visibleProcesses = processes.getItems().size();
			final int totalProcesses = runningJvms.size();
			return "Running JVMs (" + getRunningJvmDisplay(visibleProcesses, totalProcesses) + ")";
		}, processes.getItems(), searchJvms.textProperty(), runningJvms));
	}

	private String getRunningJvmDisplay(int visibleProcesses, int totalProcesses) {
		if (visibleProcesses == totalProcesses) {
			return numberFormat.format(totalProcesses);
		}
		return numberFormat.format(visibleProcesses) + "/" + numberFormat.format(totalProcesses);
	}

	private void scheduleRunningJvmUpdater() {
		executorService.scheduleAtFixedRate(() -> {
			final List<RunningJvm> activeJvms = runningJvmLoader.list();
			Platform.runLater(() -> {
				runningJvms.removeIf(savedJvm -> {
					final boolean remove = !activeJvms.contains(savedJvm);
					// Don't automatically select another JVM when the current one is removed
					if (remove && savedJvm.equals(currentJvm.get())) {
						currentJvm.set(null);
					}
					return remove;
				});
				activeJvms.stream().filter(runningJvm -> !runningJvms.contains(runningJvm)).forEach(runningJvms::add);
			});
		}, 0, 1, TimeUnit.SECONDS);
	}

}
