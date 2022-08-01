package com.github.naton1.jvmexplorer.bytecode;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.asmtools.jdis.Main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;

@Slf4j
public class OpenJdkJasmDisassembler implements Disassembler {

	@Override
	public String process(byte[] bytecode) {
		try {
			final File tmpFile = File.createTempFile("disassemble", ".class");
			Files.write(tmpFile.toPath(), bytecode);
			final StringWriter outStringWriter = new StringWriter();
			final PrintWriter out = new PrintWriter(outStringWriter);
			final StringWriter errStringWriter = new StringWriter();
			final PrintWriter err = new PrintWriter(errStringWriter);
			final Main main = new Main(out, err, "jdis");
			final String[] args = { tmpFile.getAbsolutePath() };
			final boolean result = main.disasm(args);
			if (!result) {
				throw new IllegalStateException(outStringWriter + System.lineSeparator() + errStringWriter);
			}
			return outStringWriter.toString();
		}
		catch (IOException | IllegalStateException e) {
			log.warn("Disassembly failed", e);
			return null;
		}
	}

}
