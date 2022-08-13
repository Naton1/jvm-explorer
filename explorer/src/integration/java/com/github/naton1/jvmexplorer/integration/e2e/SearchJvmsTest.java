package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.integration.helper.FxRobotPlus;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputControl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;

@Slf4j
class SearchJvmsTest extends EndToEndTest {

	@Test
	void testSearchJvms(FxRobot fxRobot) throws Exception {
		final FxRobotPlus fxRobotPlus = new FxRobotPlus(fxRobot);
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			final ListView<RunningJvm> jvms = fxRobot.lookup("#processes").queryListView();
			final TextInputControl searchField = fxRobotPlus.lookup("#searchJvms").queryTextInputControl();
			// sanity check to make sure filtering actually changes the size (should be at least 2)
			fxRobotPlus.waitUntil(() -> jvms.getItems().size() > 1, 3000);
			fxRobotPlus.interact(() -> searchField.setText(testJvm.getMainClassName()));
			fxRobotPlus.waitUntil(() -> jvms.getItems().size() == 1, 3000);
			Assertions.assertTrue(jvms.getItems()
			                          .stream()
			                          .allMatch(r -> r.toString().contains(testJvm.getMainClassName())));
		}
	}

}
