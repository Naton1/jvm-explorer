package com.github.naton1.jvmexplorer.bytecode;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.asmtools.jasm.Main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class OpenJdkJasmAssembler implements Assembler {

	@Override
	public byte[] assemble(String text) {
		try {
			final Path tempDirectory = Files.createTempDirectory("jasm");
			final File tmpFile = File.createTempFile("assemble", ".jasm");
			Files.write(tmpFile.toPath(), text.getBytes());
			final StringWriter stringWriter = new StringWriter();
			final PrintWriter out = new PrintWriter(stringWriter);
			final Main main = new Main(out, "jasm");
			final String[] args = { "-d", tempDirectory.toAbsolutePath().toString(), tmpFile.getAbsolutePath() };
			final boolean result = main.compile(args);
			if (!result) {
				throw new IllegalStateException(stringWriter.toString());
			}
			final Path outputFile = Files.walk(tempDirectory)
			                             .filter(p -> p.toFile().isFile())
			                             .findFirst()
			                             .orElseThrow(() -> new IllegalStateException(
					                             "Failed to assemble file: " + stringWriter));
			return Files.readAllBytes(outputFile);
		}
		catch (IOException | IllegalStateException e) {
			log.warn("Assembly failed", e);
			throw new AssemblyException(e);
		}
	}

}
