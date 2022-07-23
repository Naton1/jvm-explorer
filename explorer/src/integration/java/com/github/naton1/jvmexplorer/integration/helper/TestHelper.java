package com.github.naton1.jvmexplorer.integration.helper;

import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.testfx.api.FxRobot;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class TestHelper {

	public static <T> T waitFor(Supplier<T> supplier, long timeoutMs) throws InterruptedException {
		final long end = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < end) {
			final T result = supplier.get();
			if (result != null) {
				return result;
			}
			Thread.sleep(10);
		}
		throw new IllegalStateException("No result found after " + timeoutMs + " ms");
	}

	public static void waitUntil(FxRobot fxRobot, BooleanSupplier condition, long timeoutMs) throws RuntimeException {
		final long end = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < end) {
			final AtomicBoolean success = new AtomicBoolean(false);
			fxRobot.interact(() -> success.set(condition.getAsBoolean()));
			if (success.get()) {
				return;
			}
			fxRobot.sleep(5);
		}
		throw new IllegalStateException("Condition not reached - follow stack trace to see condition");
	}

	public static <T> boolean select(ListView<T> listView, String name) {
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

	public static <T> boolean select(TreeView<T> treeView, String name) {
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

}
