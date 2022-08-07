package com.github.naton1.jvmexplorer.integration.helper;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.testfx.api.FxRobot;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class FxRobotPlus extends FxRobot {

	@Delegate
	private final FxRobot fxRobot;

	public <T> void selectContextMenu(ListView<T> listView, Predicate<T> cellSelector, String action) {
		waitUntil(() -> !listView.lookupAll(".cell").isEmpty(), 5000);
		interact(() -> {
			final ListCell<T> listCell = listView.lookupAll(".cell")
			                                     .stream()
			                                     .map(n -> (ListCell<T>) n)
			                                     .filter(cell -> cellSelector.test(cell.getItem()))
			                                     .findFirst()
			                                     .orElseThrow();
			listCell.getContextMenu()
			        .getItems()
			        .stream()
			        .filter(m -> m.getText() != null && m.getText().startsWith(action))
			        .findFirst()
			        .orElseThrow()
			        .fire();
		});
	}

	public <T> void selectContextMenu(TreeView<T> treeView, String action) {
		interact(() -> {
			final TreeItem<T> selected = treeView.getSelectionModel().getSelectedItem();
			final TreeCell<T> treeCell = treeView.lookupAll(".cell")
			                                     .stream()
			                                     .map(n -> (TreeCell<T>) n)
			                                     .filter(cell -> selected.getValue().equals(cell.getItem()))
			                                     .findFirst()
			                                     .orElseThrow();
			treeCell.getContextMenu()
			        .getItems()
			        .stream()
			        .filter(m -> m.getText() != null && m.getText().startsWith(action))
			        .findFirst()
			        .orElseThrow()
			        .fire();
		});
	}

	public <T> boolean select(ListView<T> listView, String name) {
		final T process = listView.getItems()
		                          .stream()
		                          .filter(p -> p.toString().contains(name))
		                          .findFirst()
		                          .orElse(null);
		if (process == null) {
			return false;
		}
		listView.getSelectionModel().select(process);
		return true;
	}

	public <T> boolean select(TreeView<T> treeView, String name) {
		final Queue<TreeItem<T>> next = new ArrayDeque<>();
		next.add(treeView.getRoot());
		while (!next.isEmpty()) {
			final TreeItem<T> nextTreeItem = next.poll();
			if (String.valueOf(nextTreeItem.getValue()).contains(name)) {
				treeView.getSelectionModel().select(nextTreeItem);
				return true;
			}
			next.addAll(nextTreeItem.getChildren());
		}
		return false;
	}

	public void waitForExists(String query) {
		waitUntil(() -> fxRobot.lookup(query).tryQuery().isPresent(), 5000);
	}

	public void waitUntil(BooleanSupplier condition, long timeoutMs) throws RuntimeException {
		final long end = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < end) {
			final AtomicBoolean success = new AtomicBoolean(false);
			interact(() -> success.set(condition.getAsBoolean()));
			if (success.get()) {
				return;
			}
			sleep(5);
		}
		throw new IllegalStateException("Condition not reached - follow stack trace to see condition");
	}

	public void waitForStageExists(String titleRegex) {
		waitUntil(() -> fxRobot.listWindows()
		                       .stream()
		                       .filter(Stage.class::isInstance)
		                       .map(Stage.class::cast)
		                       .anyMatch(w -> w.getTitle().matches(titleRegex)), 5000);
	}

	public <T> T waitFor(Supplier<T> supplier, long timeoutMs) throws RuntimeException {
		final long end = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < end) {
			final T result = supplier.get();
			if (result != null) {
				return result;
			}
			sleep(10);
		}
		throw new IllegalStateException("No result found after " + timeoutMs + " ms");
	}

}
