package com.github.naton1.jvmexplorer.bytecode;

import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayInputStream;

public class JasmDisassembler implements Disassembler {
	@Override
	public String process(byte[] bytecode) {
		final ClassReader classReader = new ClassReader(bytecode);
		final ClassNode classNode = new ClassNode();
		classReader.accept(classNode, 0);
		final String className = ClassNameHelper.getSimpleName(classNode.name.replace('/', '.'));
		final com.roscopeco.jasm.JasmDisassembler disassembler = new com.roscopeco.jasm.JasmDisassembler(className,
		                                                                                                 true,
		                                                                                                 () -> new ByteArrayInputStream(
				                                                                                                 bytecode));
		return disassembler.disassemble();
	}
}
