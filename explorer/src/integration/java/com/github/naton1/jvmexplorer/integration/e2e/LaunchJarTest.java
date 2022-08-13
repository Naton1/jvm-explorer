package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.JarTestHelper;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.agent.RunningJvmLoader;
import com.github.naton1.jvmexplorer.helper.FileHelper;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

class LaunchJarTest extends EndToEndTest {

	@Test
	void testLaunchJar() throws Exception {
		final File jar = JarTestHelper.buildJar(SleepForeverProgram.class);
		final ListView<?> listView = appHelper.getJvmList();

		FileHelper.setTestFile(fxRobot.listWindows().stream().findFirst().orElseThrow(), jar);

		final RunningJvmLoader runningJvmLoader = new RunningJvmLoader();
		final List<RunningJvm> initialJvms = runningJvmLoader.list();
		fxRobot.selectContextMenu(listView, t -> true, "Launch JAR");

		final RunningJvm loadedProgram = fxRobot.waitFor(() -> runningJvmLoader.list()
		                                                                       .stream()
		                                                                       .filter(r -> !initialJvms.contains(r))
		                                                                       .filter(j -> j.getName()
		                                                                                     .contains(jar.toString()))
		                                                                       .findFirst()
		                                                                       .orElse(null), 5000);

		// cleanup
		final long pid = Long.parseLong(loadedProgram.getId());
		ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
	}

}
