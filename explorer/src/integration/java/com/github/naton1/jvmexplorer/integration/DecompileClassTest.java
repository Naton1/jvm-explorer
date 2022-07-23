package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.JvmExplorer;
import com.github.naton1.jvmexplorer.integration.helper.TestHelper;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

@ExtendWith(ApplicationExtension.class)
@Slf4j
class DecompileClassTest {

	@BeforeEach
	void setup() throws Exception {
		FxToolkit.setupApplication(JvmExplorer.class);
	}

	@Test
	void testProcessAppears_classesLoad_classDecompiles(FxRobot fxRobot) throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			final ListView<?> listView = fxRobot.lookup("#processes").queryListView();
			TestHelper.waitUntil(fxRobot, () -> TestHelper.select(listView, testJvm.getProcess().pid() + ""), 5000);
			final TreeView<?> treeView = fxRobot.lookup("#classes").queryAs(TreeView.class);
			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			TestHelper.waitUntil(fxRobot, () -> TestHelper.select(treeView, simpleName), 5000);
			final CodeArea classFile = fxRobot.lookup("#classFile").queryAs(CodeArea.class);
			TestHelper.waitUntil(fxRobot,
			                     () -> classFile.getText().contains("class " + simpleName),
			                     5000);
		}
	}

}
