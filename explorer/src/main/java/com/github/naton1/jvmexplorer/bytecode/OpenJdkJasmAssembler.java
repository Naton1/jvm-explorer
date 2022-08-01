package com.github.naton1.jvmexplorer.bytecode;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OpenJdkJasmAssembler implements Assembler {

	@Override
	public byte[] assemble(String text) {
		try {
			final Path tempDirectory = Files.createTempDirectory("jasm");
			final String jar = OpenJdkTools.getJarLocation();
			final File tmpFile = File.createTempFile("assemble", ".jasm");
			Files.write(tmpFile.toPath(), text.getBytes());
			final Process process = new ProcessBuilder().command(
					System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
					"-jar",
					jar,
					"jasm",
					"-d",
					tempDirectory.toAbsolutePath().toString(),
					tmpFile.getAbsolutePath()).redirectErrorStream(true).start();
			final byte[] output;
			try (final InputStream inputStream = process.getInputStream()) {
				output = inputStream.readAllBytes();
			}
			// should never take this long, but let's give it some time in case
			final boolean finished = process.waitFor(30, TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new IllegalStateException("Assembler never completed");
			}
			final Path outputFile = Files.walk(tempDirectory)
			                             .filter(p -> p.toFile().isFile())
			                             .findFirst()
			                             .orElseThrow(() -> new IllegalStateException(
					                             "Failed to assemble file: " + new String(output)));
			return Files.readAllBytes(outputFile);
		}
		catch (IOException | InterruptedException | IllegalStateException e) {
			log.warn("Assembly failed", e);
			throw new AssemblyException(e);
		}
	}

}
