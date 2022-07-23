package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.agent.RunningJvmLoader;
import com.github.naton1.jvmexplorer.integration.helper.TestHelper;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

class RunningJvmTest {

	@Test
	void testSystemProperties() throws Exception {
		final RunningJvmLoader runningJvmLoader = new RunningJvmLoader();
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			final RunningJvm runningJvm = TestHelper.waitFor(() -> runningJvmLoader.list()
			                                                                       .stream()
			                                                                       .filter(r -> r.getId()
			                                                                                     .equals(String.valueOf(
					                                                                                     testJvm.getProcess()
					                                                                                            .pid())))
			                                                                       .findFirst()
			                                                                       .orElse(null), 5000);
			final Properties properties = runningJvm.getSystemProperties();
			Assertions.assertNotNull(properties);
		}
	}

}
