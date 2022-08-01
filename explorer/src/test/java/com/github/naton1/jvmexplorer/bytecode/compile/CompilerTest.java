package com.github.naton1.jvmexplorer.bytecode.compile;

import com.github.naton1.jvmexplorer.helper.RemoteCodeTemplateHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

@ExtendWith(MockitoExtension.class)
class CompilerTest {

	// These may want to be moved to integration tests because they test integration multiple components

	@Mock
	private JavacBytecodeProvider bytecodeProvider;

	@Test
	void testValidClass() {
		final Compiler compiler = new Compiler();

		final CompileResult compileResult = compiler.compile(Runtime.version().feature(),
		                                                     "Test",
		                                                     "public class Test {}",
		                                                     bytecodeProvider);

		Assertions.assertTrue(compileResult.isSuccess(), compileResult.getStdOut());
		Assertions.assertTrue(compileResult.getClassContent().length > 0);
	}

	@Test
	void testInvalidClass() {
		final Compiler compiler = new Compiler();

		final CompileResult compileResult = compiler.compile(Runtime.version().feature(),
		                                                     "Test",
		                                                     "{}",
		                                                     bytecodeProvider);

		Assertions.assertFalse(compileResult.isSuccess(), compileResult.getStdOut());
	}

	@Test
	void testTemplateClass() {
		final RemoteCodeTemplateHelper remoteCodeTemplateHelper = new RemoteCodeTemplateHelper();

		final Compiler compiler = new Compiler();

		final String classContent = remoteCodeTemplateHelper.load("test", "Test");
		final CompileResult compileResult = compiler.compile(Runtime.version().feature(),
		                                                     "Test",
		                                                     classContent,
		                                                     bytecodeProvider);

		Assertions.assertTrue(compileResult.isSuccess(), compileResult.getStdOut());
		Assertions.assertTrue(compileResult.getClassContent().length > 0);
	}

	@Test
	void testTemplateClassNoPackage() {
		final RemoteCodeTemplateHelper remoteCodeTemplateHelper = new RemoteCodeTemplateHelper();

		final Compiler compiler = new Compiler();

		final String classContent = remoteCodeTemplateHelper.load(null, "Test");
		final CompileResult compileResult = compiler.compile(Runtime.version().feature(),
		                                                     "Test",
		                                                     classContent,
		                                                     bytecodeProvider);

		Assertions.assertTrue(compileResult.isSuccess(), compileResult.getStdOut());
		Assertions.assertTrue(compileResult.getClassContent().length > 0);
	}

	@Test
	void testSourceCompatibility() {
		final Compiler compiler = new Compiler();

		// Target java 8. This application only runs on java 11+ so it must target a lower version.
		final CompileResult compileResult = compiler.compile(8, "Test", "public class Test {}", bytecodeProvider);

		Assertions.assertTrue(compileResult.isSuccess(), compileResult.getStdOut());
		Assertions.assertTrue(compileResult.getClassContent().length > 0);

		final ClassReader classReader = new ClassReader(compileResult.getClassContent());
		final ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);

		Assertions.assertEquals(Opcodes.V1_8, classNode.version & 0xFFFF);
		Assertions.assertEquals("Test", classNode.name);
	}

}