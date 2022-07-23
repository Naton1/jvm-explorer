package com.github.naton1.jvmexplorer.integration.helper;

import lombok.Builder;
import lombok.Singular;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestJvm implements AutoCloseable {

	private final String mainClassName;
	private final Process process;

	@Builder
	private TestJvm(Class<?> sourceClass, @Singular List<String> jvmArgs, @Singular List<String> programArgs,
	                boolean handleIOManually)
			throws Exception {
		this.mainClassName = sourceClass.getName();
		final URL base = sourceClass.getProtectionDomain().getCodeSource().getLocation();
		final File workingDirectory = new File(base.toURI());
		final List<String> command = Stream.of(List.of(
				                                       System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"),
		                                       jvmArgs,
		                                       List.of(this.mainClassName),
		                                       programArgs).flatMap(Collection::stream).collect(Collectors.toList());
		final ProcessBuilder processBuilder = new ProcessBuilder().command(command).directory(workingDirectory);
		if (!handleIOManually) {
			processBuilder.inheritIO();
		}
		else {
			// There is no input mechanism
			// Also send error to std out to make it easier
			processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT)
					.redirectErrorStream(true);
		}
		this.process = processBuilder.start();
		System.out.println("Launched JVM with class: " + sourceClass + ", pid: " + this.process.pid());
	}

	@Override
	public void close() {
		process.destroy();
		try {
			final boolean ended = process.waitFor(2, TimeUnit.SECONDS);
			if (ended) {
				return;
			}
		}
		catch (InterruptedException ignored) {
		}
		process.destroyForcibly();
	}

	public String getMainClassName() {
		return this.mainClassName;
	}

	public Process getProcess() {
		return process;
	}

	public static TestJvm of(Class<?> klass) throws Exception {
		return TestJvm.builder().sourceClass(klass).build();
	}

}
