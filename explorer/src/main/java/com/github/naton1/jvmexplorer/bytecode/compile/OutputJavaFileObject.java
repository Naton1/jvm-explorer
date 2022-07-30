package com.github.naton1.jvmexplorer.bytecode.compile;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

public class OutputJavaFileObject extends SimpleJavaFileObject {

	private final ByteArrayOutputStream os = new ByteArrayOutputStream();

	public OutputJavaFileObject(String name, JavaFileObject.Kind kind) {
		super(URI.create("memory:///" + name.replace('.', '/') + kind.extension), kind);
	}

	public byte[] getBytes() {
		return this.os.toByteArray();
	}

	@Override
	public OutputStream openOutputStream() {
		return this.os;
	}

}
