package com.github.naton1.jvmexplorer.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

abstract class AssemblyTest {

	@Test
	void givenValidClass_whenDisassemble_classDisassembled() throws IOException {
		final byte[] classFile = Objects.requireNonNull(getClass().getClassLoader()
		                                                          .getResourceAsStream("SleepForever.class"))
		                                .readAllBytes();

		final Disassembler disassembler = getDisassembler();
		final String disassembledClassFile = disassembler.process(classFile);

		Assertions.assertTrue(disassembledClassFile.contains("public class SleepForever"));
	}

	abstract Disassembler getDisassembler();

	@Test
	void givenValidClass_whenDisassembleThenAssemble_thenResultValid() throws Exception {
		final byte[] classFile = Objects.requireNonNull(getClass().getClassLoader()
		                                                          .getResourceAsStream("SleepForever.class"))
		                                .readAllBytes();

		final Disassembler disassembler = getDisassembler();
		final String disassembledClassFile = disassembler.process(classFile);

		final Assembler assembler = getAssembler();
		final byte[] assembledClassFile = assembler.assemble(disassembledClassFile);

		Assertions.assertTrue(assembledClassFile.length > 0);

		// Let's verify we can load it
		final Class<?> klass = new ClassLoader() {
			public Class<?> findClass(String name) {
				return defineClass(name, assembledClassFile, 0, assembledClassFile.length);
			}
		}.loadClass("SleepForever");
		Assertions.assertNotNull(klass);
	}

	abstract Assembler getAssembler();

}