package com.github.naton1.jvmexplorer.agent;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AgentPreparer {

	private final Map<String, String> tempAgentFiles = new ConcurrentHashMap<>();

	public String loadAgentOnFileSystem(String agentResourcePath) {
		return tempAgentFiles.computeIfAbsent(agentResourcePath, k -> {
			try {
				final Path tempFile = Files.createTempFile("agent", ".jar");
				tempFile.toFile().deleteOnExit();
				final InputStream inputStream = AgentPreparer.class.getClassLoader()
				                                                   .getResourceAsStream(agentResourcePath);
				if (inputStream == null) {
					throw new IOException("Failed to find input stream for agent");
				}
				final byte[] agentBytes = inputStream.readAllBytes();
				Files.write(tempFile, agentBytes);
				final String localPath = tempFile.toAbsolutePath().toString();
				log.debug("Loaded agent into temp file: {}", localPath);
				return localPath;
			}
			catch (IOException e) {
				log.warn("Failed to load agent into file", e);
				throw new UncheckedIOException(e);
			}
		});

	}

}
