package com.github.naton1.jvmexplorer.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

class JasmDisassemblerTest {

	// These are kind of testing a third party library, but it gives us guarantees that the adapter will work
	// and that we can disassemble and reassemble without issue.

	@Test
	void givenValidClass_whenDisassemble_classDisassembled() throws IOException {
		final byte[] classFile = Objects.requireNonNull(getClass().getClassLoader()
		                                                          .getResourceAsStream("SleepForever.class"))
		                                .readAllBytes();

		final JasmDisassembler jasmDisassembler = new JasmDisassembler();
		final String disassembledClassFile = jasmDisassembler.process(classFile);

		Assertions.assertTrue(disassembledClassFile.contains("public class SleepForever"));
	}

	@Test
	void givenValidClass_whenDisassembleThenAssemble_thenResultValid() throws IOException {
		final byte[] classFile = Objects.requireNonNull(getClass().getClassLoader()
		                                                          .getResourceAsStream("SleepForever.class"))
		                                .readAllBytes();

		final JasmDisassembler asmDisassembler = new JasmDisassembler();
		final String disassembledClassFile = asmDisassembler.process(classFile);

		final JasmAssembler jasmAssembler = new JasmAssembler("SleepForever");
		final byte[] assembledClassFile = jasmAssembler.assemble(disassembledClassFile);

		Assertions.assertTrue(assembledClassFile.length > 0);
	}

}