package com.github.naton1.jvmexplorer.bytecode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class AsmDisassembler implements Disassembler {

	public String process(byte[] bytes) {
		final ClassReader classReader = new ClassReader(bytes);
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		final ClassVisitor traceClassVisitor = new TraceClassVisitor(printWriter);
		classReader.accept(traceClassVisitor, 0);
		return stringWriter.toString().trim();
	}

}
