package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.agent.AgentPreparer;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.agent.RunningJvmLoader;
import com.github.naton1.jvmexplorer.integration.helper.TestHelper;
import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.LaunchAnotherJvmProgram;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

class LaunchPatchAgentTest {

	// This is a sanity test to ensure we normally can't attach
	@Test
	void verifyAttachFailsNormally() throws Exception {
		try (final TestJvm testJvm = TestJvm.builder()
		                                    .sourceClass(LaunchAnotherJvmProgram.class)
		                                    .handleIOManually(true)
		                                    .build()
		) {
			final long pid = getLaunchedProcessId(testJvm);
			Assertions.assertThrows(Exception.class, () -> findLaunchedJvm(pid));
			ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
		}
	}

	@Test
	void verifyAttachPatch() throws Exception {
		final AgentPreparer agentPreparer = new AgentPreparer();
		final String agentPath = agentPreparer.loadAgentOnFileSystem("agents/launch-agent.jar");
		try (final TestJvm testJvm = TestJvm.builder()
		                                    .sourceClass(LaunchAnotherJvmProgram.class)
		                                    .handleIOManually(true)
		                                    .jvmArg("-javaagent" + ":" + agentPath)
		                                    .build()
		) {
			final long pid = getLaunchedProcessId(testJvm);
			final RunningJvm runningJvm = findLaunchedJvm(pid);
			final Properties properties = runningJvm.getSystemProperties();
			Assertions.assertNotNull(properties);
			runningJvm.loadAgent(agentPath, null);
			ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
		}
	}

	private long getLaunchedProcessId(TestJvm testJvm) throws Exception {
		try (final BufferedReader br = new BufferedReader(new InputStreamReader(testJvm.getProcess().getInputStream()))
		) {
			return br.lines().filter(s -> s.matches("\\d+")).findFirst().map(Long::parseLong).orElseThrow();
		}
	}

	private RunningJvm findLaunchedJvm(long pid) throws InterruptedException {
		final RunningJvmLoader runningJvmLoader = new RunningJvmLoader();
		return TestHelper.waitFor(() -> runningJvmLoader.list()
		                                                .stream()
		                                                .filter(r -> r.getId().equals(String.valueOf(pid)))
		                                                .findFirst()
		                                                .orElse(null), 5000);
	}

}
