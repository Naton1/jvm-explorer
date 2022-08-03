package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.FxRobotPlus;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;

@Slf4j
class DecompileClassTest extends EndToEndTest {

	@Test
	void testProcessAppears_classesLoad_classDecompiles(FxRobot fxRobot) throws Exception {
		final FxRobotPlus fxRobotPlus = new FxRobotPlus(fxRobot);
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			final ListView<?> listView = fxRobotPlus.lookup("#processes").queryListView();
			fxRobotPlus.waitUntil(() -> fxRobotPlus.select(listView, testJvm.getProcess().pid() + ""), 5000);
			final TreeView<?> treeView = fxRobotPlus.lookup("#classes").queryAs(TreeView.class);
			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			fxRobotPlus.waitUntil(() -> fxRobotPlus.select(treeView, simpleName), 5000);

			// Verify decompile
			final CodeArea classFile = fxRobotPlus.lookup("#classFile").queryAs(CodeArea.class);
			fxRobotPlus.waitUntil(() -> classFile.getText().contains("class " + simpleName), 5000);

			// Verify disassemble
			final TabPane tabPane = fxRobotPlus.lookup("#currentClassTabPane").queryAs(TabPane.class);
			tabPane.getSelectionModel().select(1); // select bytecode
			final CodeArea bytecode = fxRobotPlus.lookup("#bytecode").queryAs(CodeArea.class);
			fxRobotPlus.waitUntil(() -> classFile.getText().contains("class " + simpleName), 5000);

			// Verify fields
			tabPane.getSelectionModel().select(2); // select fields
			final TreeView<?> classFields = fxRobotPlus.lookup("#classFields").queryAs(TreeView.class);
			Assertions.assertTrue(classFields.getRoot().getChildren().size() > 0);
		}
	}

}
