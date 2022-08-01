package com.github.naton1.jvmexplorer.bytecode;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

@Slf4j
public class OpenJdkJasmDisassembler implements Disassembler {

	@Override
	public String process(byte[] bytecode) {
		try {
			final String jar = OpenJdkTools.getJarLocation();
			final File tmpFile = File.createTempFile("disassemble", ".class");
			Files.write(tmpFile.toPath(), bytecode);
			final Process process = new ProcessBuilder().command(
					System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
					"-jar",
					jar,
					"jdis",
					tmpFile.getAbsolutePath()).redirectErrorStream(true).start();
			try (BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				return stdout.lines().collect(Collectors.joining(System.lineSeparator()));
			}
			finally {
				// Make sure it ends no matter what
				process.destroyForcibly();
			}
		}
		catch (IOException e) {
			log.warn("Disassembly failed", e);
			return null;
		}
	}

}
