package com.github.naton1.jvmexplorer.protocol;

import lombok.Builder;
import lombok.Value;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

@Value
@Builder
public class AgentConfiguration {

	private static final String PORT_KEY = "port";
	private static final String IDENTIFIER_KEY = "identifier";
	private static final String HOST_NAME_KEY = "hostName";
	private static final String LOG_LEVEL_KEY = "logLevel";

	private final int port;
	private final String identifier;
	private final String hostName;
	private final int logLevel;

	public String toAgentArgs() {
		final Properties properties = new Properties();
		properties.setProperty(PORT_KEY, Integer.toString(port));
		properties.setProperty(IDENTIFIER_KEY, identifier);
		properties.setProperty(HOST_NAME_KEY, hostName);
		properties.setProperty(LOG_LEVEL_KEY, Integer.toString(logLevel));
		final StringWriter stringWriter = new StringWriter();
		try {
			properties.store(stringWriter, null);
		}
		catch (IOException e) {
			// Should never happen
			throw new IllegalStateException(e);
		}
		return stringWriter.toString();
	}

	public static AgentConfiguration parseAgentArgs(String agentArgs) {
		final Properties properties = new Properties();
		try {
			properties.load(new StringReader(agentArgs));
		}
		catch (IOException e) {
			// Should never happen
			throw new IllegalStateException(e);
		}
		return AgentConfiguration.builder()
		                         .port(Integer.parseInt(properties.getProperty(PORT_KEY)))
		                         .hostName(properties.getProperty(HOST_NAME_KEY))
		                         .identifier(properties.getProperty(IDENTIFIER_KEY))
		                         .logLevel(Integer.parseInt(properties.getProperty(LOG_LEVEL_KEY)))
		                         .build();
	}

}
