package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.integration.helper.FxRobotPlus;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import javafx.scene.control.ListView;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;

@Slf4j
class ViewSystemPropertyTest extends EndToEndTest {

	@Test
	void testShowSystemProperties(FxRobot fxRobot) throws Exception {
		final FxRobotPlus fxRobotPlus = new FxRobotPlus(fxRobot);
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			final ListView<RunningJvm> jvms = fxRobot.lookup("#processes").queryListView();
			fxRobotPlus.selectContextMenu(jvms,
			                              item -> item != null && item.getId().equals(testJvm.getProcess().pid() + ""),
			                              "View System Properties");
			fxRobotPlus.waitForStageExists("JVM System Properties");
			final ListView<String> properties = findPropertiesListView(fxRobotPlus);
			log.debug("Found {} system properties in {}", properties.getItems().size(), testJvm.getProcess().pid());
			Assertions.assertFalse(properties.getItems().isEmpty());
		}
	}

	private ListView<String> findPropertiesListView(FxRobotPlus fxRobotPlus) {
		return fxRobotPlus.targetWindow("JVM System Properties")
		                  .lookup(".list-view")
		                  .match(t -> t instanceof ListView && ((ListView<?>) t).getItems()
		                                                                        .stream()
		                                                                        .map(Object::toString)
		                                                                        .anyMatch(s -> s.startsWith("java.home")))
		                  .queryListView();
	}

}
