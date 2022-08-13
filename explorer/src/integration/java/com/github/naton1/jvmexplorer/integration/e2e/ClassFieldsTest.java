package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeView;
import javafx.scene.input.Clipboard;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Slf4j
class ClassFieldsTest extends EndToEndTest {

	@Test
	void testNestedFieldsShow() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.openTab(2);
			appHelper.waitUntilDisassemble(); // not directly related, but means fields are loaded too
			final TreeView<?> fieldTree = appHelper.getFieldTree();
			// Verify there is a nested field
			fieldTree.getRoot()
			         .getChildren()
			         .stream()
			         .filter(c -> c.getChildren().size() > 0) // verify it has nested fields
			         .filter(c -> c.getValue().toString().contains("private")) // make sure it's reading a private
			         // field
			         .filter(c -> c.getValue().toString().contains("List"))
			         .findFirst()
			         .orElseThrow();
		}
	}

	@Test
	void testCopyField() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.openTab(2);
			appHelper.waitUntilDisassemble(); // not directly related, but means fields are loaded too
			final TreeView<?> fieldTree = appHelper.getFieldTree();
			// Verify there is a nested field
			fxRobot.interact(() -> fieldTree.getSelectionModel().selectFirst());
			fxRobot.selectContextMenu(fieldTree, "Copy");

			fxRobot.interact(() -> {
				final String text = fieldTree.getSelectionModel().getSelectedItem().getValue().toString();
				final String clipboard = Clipboard.getSystemClipboard().getString();

				Assertions.assertTrue(text.matches(".* = " + Pattern.quote(clipboard)));
			});
		}
	}

	@Test
	@Timeout(30)
	void testModifyField() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.openTab(2);
			final TreeView<?> fieldTree = appHelper.getFieldTree();
			fxRobot.interact(() -> fieldTree.getSelectionModel().selectFirst());
			final AtomicBoolean success = new AtomicBoolean(false);
			final Thread thread = new Thread(() -> {
				// Dialog is blocking
				fxRobot.waitForStageExists("Update Field");
				final TextInputControl textField = fxRobot.targetWindow("Update Field")
				                                          .lookup(".text-field")
				                                          .match(t -> t instanceof TextInputControl
				                                                      && ((TextInputControl) t).getText().equals("15"))
				                                          .queryTextInputControl();
				fxRobot.interact(() -> {
					textField.setText("1000");
					fxRobot.targetWindow("Update Field").lookup("OK").queryButton().fire();
				});
				fxRobot.waitUntil(() -> fieldTree.getSelectionModel()
				                                 .getSelectedItem()
				                                 .getValue()
				                                 .toString()
				                                 .contains("1000"), 5000);
				success.set(true);
			});
			thread.setUncaughtExceptionHandler((t, e) -> log.warn("Async thread failed", e));
			thread.start();
			fxRobot.selectContextMenu(fieldTree, "Edit Value");
			thread.join();
			Assertions.assertTrue(success.get());
		}
	}

}
