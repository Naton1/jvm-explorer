package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.fx.classes.ClassTreeNode;
import com.github.naton1.jvmexplorer.integration.FxRobotPlus;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeView;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
class RemoteCodeExecutionTest extends EndToEndTest {

	@Test
	void testExecuteRemoteCode(FxRobot fxRobot) throws Exception {
		final FxRobotPlus fxRobotPlus = new FxRobotPlus(fxRobot);
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			final ListView<?> listView = fxRobot.lookup("#processes").queryListView();
			fxRobotPlus.waitUntil(() -> fxRobotPlus.select(listView, testJvm.getProcess().pid() + ""), 5000);
			final TreeView<ClassTreeNode> treeView = fxRobotPlus.lookup("#classes").queryAs(TreeView.class);
			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			fxRobotPlus.waitUntil(() -> fxRobotPlus.select(treeView, simpleName), 5000);
			fxRobotPlus.selectContextMenu(treeView, "Run Code In Package");
			fxRobotPlus.sleep(1000);
			fxRobotPlus.targetWindow("Remote Code Executor.*").clickOn("Execute Code");

			final TextInputControl textArea = fxRobotPlus.targetWindow("Remote Code Executor.*")
			                                             .lookup(TextArea.class::isInstance)
			                                             .queryTextInputControl();

			WaitForAsyncUtils.waitFor(5000, TimeUnit.MILLISECONDS, () -> textArea.getText().contains("Succeeded"));

			Assertions.assertTrue(textArea.getText().contains("Hello world from " + testJvm.getProcess().pid()));
		}
	}

}
