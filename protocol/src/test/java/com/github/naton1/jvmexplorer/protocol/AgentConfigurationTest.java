package com.github.naton1.jvmexplorer.protocol;

import org.junit.Assert;
import org.junit.Test;

public class AgentConfigurationTest {

	@Test
	public void givenAgentConfig_whenToAgentArgsThenParseArgs_parsedArgsEqualsInitialConfig() {
		final AgentConfiguration agentConfiguration = AgentConfiguration.builder()
		                                                                .identifier("id")
		                                                                .logLevel(1)
		                                                                .port(5000)
		                                                                .hostName("1.1.1.1")
		                                                                .logFilePath("log-file-path")
		                                                                .build();
		final String args = agentConfiguration.toAgentArgs();
		final AgentConfiguration parsedAgentConfigured = AgentConfiguration.parseAgentArgs(args);
		Assert.assertEquals(agentConfiguration, parsedAgentConfigured);
	}

}