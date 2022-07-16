package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.JvmExplorer;
import com.github.naton1.jvmexplorer.integration.helper.TestHelper;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
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
public class DecompileClassTest {

	@BeforeEach
	public void setup() throws Exception {
		FxToolkit.setupApplication(JvmExplorer.class);
	}

	@Test
	public void testProcessAppears_classesLoad_classDecompiles(FxRobot fxRobot) throws Exception {
		try (final TestJvm testJvm = new TestJvm()) {
			final ListView<?> listView = fxRobot.lookup("#processes").queryListView();
			TestHelper.waitUntil(fxRobot, () -> TestHelper.select(listView, testJvm.getMainClassName()), 5000);
			final TreeView<?> treeView = fxRobot.lookup("#classes").queryAs(TreeView.class);
			TestHelper.waitUntil(fxRobot, () -> TestHelper.select(treeView, testJvm.getMainClassName()), 5000);
			final CodeArea classFile = fxRobot.lookup("#classFile").queryAs(CodeArea.class);
			TestHelper.waitUntil(fxRobot,
			                     () -> classFile.getText().contains("class " + testJvm.getMainClassName()),
			                     5000);
		}
	}

}
