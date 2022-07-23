package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.agent.AgentPreparer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class AgentPreparerTest {

	@Test
	void testPrepareAgent() throws IOException {
		final AgentPreparer agentPreparer = new AgentPreparer();
		final String output = agentPreparer.loadAgentOnFileSystem("agents/agent.jar");

		Assertions.assertNotNull(output);

		Files.delete(Path.of(output)); // Verify file exists and clean up at same time
	}

}
