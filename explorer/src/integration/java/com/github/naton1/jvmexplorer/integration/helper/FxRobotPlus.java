package com.github.naton1.jvmexplorer.integration.helper;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.testfx.api.FxRobot;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Slf4j
public class FxRobotPlus extends FxRobot {

	@Delegate
	private final FxRobot fxRobot;

	public void selectContextMenu(ContextMenu contextMenu, String action) {
		log.debug("Selecting context menu item with {} in {}", action, contextMenu);
		waitUntil(() -> {
			final MenuItem menuItem = getMenuItem(contextMenu, action);
			fire(menuItem);
		}, 5000);
	}

	public void selectContextMenu(Control control, String action) {
		log.debug("Selecting context menu item with {} in {}", action, control);
		waitUntil(() -> {
			final MenuItem menuItem = getMenuItem(control.getContextMenu(), action);
			fire(menuItem);
		}, 5000);
	}

	public <T> void selectContextMenu(ListView<T> listView, String action) {
		final T selected = listView.getSelectionModel().getSelectedItem();
		log.debug("Selecting context menu item with {} in {} (selectedItem={})", action, listView, selected);
		waitUntil(() -> {
			listView.scrollTo(selected);
			final MenuItem menuItem = listView.lookupAll(".cell")
			                                  .stream()
			                                  .map(n -> (ListCell<T>) n)
			                                  .filter(cell -> selected == null || selected.equals(cell.getItem()))
			                                  .map(cell -> getMenuItem(cell.getContextMenu(), action))
			                                  .filter(Objects::nonNull)
			                                  .findFirst()
			                                  .orElseThrow();
			fire(menuItem);
		}, 5000);
	}

	public <T> void selectContextMenu(ListView<T> listView, Predicate<T> cellSelector, String action) {
		log.debug("Selecting context menu item with {} in {}", action, listView);
		waitUntil(() -> {
			final MenuItem menuItem = listView.lookupAll(".cell")
			                                  .stream()
			                                  .map(n -> (ListCell<T>) n)
			                                  .filter(cell -> cellSelector.test(cell.getItem()))
			                                  .map(cell -> getMenuItem(cell.getContextMenu(), action))
			                                  .filter(Objects::nonNull)
			                                  .findFirst()
			                                  .orElseThrow();
			fire(menuItem);
		}, 5000);
	}

	public <T> void selectContextMenu(TreeView<T> treeView, String action) {
		final TreeItem<T> selected = treeView.getSelectionModel().getSelectedItem();
		log.debug("Selecting context menu item with {} in {} (selectedItem={})", action, treeView, selected);
		waitUntil(() -> {
			final int selectedIndex = treeView.getSelectionModel().getSelectedIndex();
			treeView.scrollTo(selectedIndex);
			final MenuItem menuItem = treeView.lookupAll(".cell")
			                                  .stream()
			                                  .map(n -> (TreeCell<T>) n)
			                                  .filter(c -> c.getContextMenu() != null
			                                               && c.getContextMenu().getItems().size() > 0)
			                                  .filter(cell -> selected == null || selected.getValue()
			                                                                              .equals(cell.getItem()))
			                                  .map(treeCell -> getMenuItem(treeCell.getContextMenu(), action))
			                                  .filter(Objects::nonNull)
			                                  .findFirst()
			                                  .orElseThrow();
			fire(menuItem);
		}, 5000);
	}

	private MenuItem getMenuItem(ContextMenu contextMenu, String action) {
		return contextMenu.getItems()
		                  .stream()
		                  .filter(m -> m.getText() != null && m.getText().startsWith(action))
		                  .findFirst()
		                  .orElse(null);
	}

	private void fire(MenuItem menuItem) {
		if (menuItem instanceof CheckMenuItem) {
			// JavaFX doesn't automatically do this through fire()...
			final CheckMenuItem checkMenuItem = (CheckMenuItem) menuItem;
			checkMenuItem.setSelected(!checkMenuItem.isSelected());
		}
		menuItem.fire();
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

	public void waitUntil(Runnable action, long timeoutMs) throws RuntimeException {
		waitUntil(() -> {
			try {
				action.run();
				return true;
			}
			catch (Exception e) {
				log.debug("Test attempt failed with {}", e.getClass().getName() + ": " + e.getMessage());
				return false;
			}
		}, timeoutMs);
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

	public <T> void selectComboBox(ComboBox<T> comboBox, String text) {
		waitUntil(() -> {
			final T comboBoxItem = comboBox.getItems()
			                               .stream()
			                               .filter(item -> item.toString().contains(text))
			                               .findFirst()
			                               .orElseThrow();
			comboBox.getSelectionModel().select(comboBoxItem);
		}, 5000);
	}

}
