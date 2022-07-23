package com.github.naton1.jvmexplorer.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;

class QuiltflowerDecompilerTest {

	@Test
	void givenValidClass_whenDecompile_classDecompiled() throws IOException {
		final byte[] classFile = Objects.requireNonNull(getClass().getClassLoader()
		                                                          .getResourceAsStream("SleepForever.class"))
		                                .readAllBytes();

		final QuiltflowerDecompiler quiltflowerDecompiler = new QuiltflowerDecompiler();
		final String decompiledClassFile = quiltflowerDecompiler.process(classFile);

		Assertions.assertTrue(decompiledClassFile.contains("public class SleepForever"));
	}

}