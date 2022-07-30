package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.agent.RunningJvmLoader;
import com.github.naton1.jvmexplorer.integration.helper.TestHelper;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

class RunningJvmLoaderTest {

	@Test
	void testIgnoreCurrentProcess() {
		final RunningJvmLoader runningJvmLoader = new RunningJvmLoader();
		final String pid = ManagementFactory.getRuntimeMXBean().getPid() + "";
		final boolean currentProcessIsIgnored = runningJvmLoader.list().stream().noneMatch(j -> pid.equals(j.getId()));
		Assertions.assertTrue(currentProcessIsIgnored);
	}

	@Test
	void testShowsLoadedProcess() throws Exception {
		final RunningJvmLoader runningJvmLoader = new RunningJvmLoader();
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			TestHelper.waitFor(() -> runningJvmLoader.list()
			                                         .stream()
			                                         .filter(r -> r.getId()
			                                                       .equals(String.valueOf(testJvm.getProcess().pid())))
			                                         .findFirst()
			                                         .orElse(null), 5000);
		}
	}

}
