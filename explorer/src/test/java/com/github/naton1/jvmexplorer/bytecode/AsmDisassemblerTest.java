package com.github.naton1.jvmexplorer.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

class AsmDisassemblerTest {

	@Test
	void givenValidClass_whenDisassemble_classDisassembled() throws IOException {
		final byte[] classFile = Objects.requireNonNull(getClass().getClassLoader()
		                                                          .getResourceAsStream("SleepForever.class"))
		                                .readAllBytes();

		final AsmDisassembler asmDisassembler = new AsmDisassembler();
		final String disassembledClassFile = asmDisassembler.process(classFile);

		Assertions.assertTrue(disassembledClassFile.contains("public class SleepForever"));
	}

}