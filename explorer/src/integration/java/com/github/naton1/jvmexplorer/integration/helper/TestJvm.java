package com.github.naton1.jvmexplorer.integration.helper;

import java.io.File;
import java.net.URL;
import java.util.Objects;

public class TestJvm implements AutoCloseable {

	private static final String LAUNCHED_PROCESS_MAIN_CLASS_NAME = "SleepForever";

	private final Process process;

	public TestJvm() throws Exception {
		final URL resource = getClass().getClassLoader().getResource(LAUNCHED_PROCESS_MAIN_CLASS_NAME + ".class");
		final File sleepForeverFile = new File(Objects.requireNonNull(resource).toURI());
		this.process = new ProcessBuilder().command(
				System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
				LAUNCHED_PROCESS_MAIN_CLASS_NAME).directory(sleepForeverFile.getParentFile()).inheritIO().start();
	}

	@Override
	public void close() {
		process.destroyForcibly();
	}

	public String getMainClassName() {
		return LAUNCHED_PROCESS_MAIN_CLASS_NAME;
	}

}
