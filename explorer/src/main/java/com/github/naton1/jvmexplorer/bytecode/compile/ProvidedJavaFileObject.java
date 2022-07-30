package com.github.naton1.jvmexplorer.bytecode.compile;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

public class ProvidedJavaFileObject extends SimpleJavaFileObject {

	private final Supplier<byte[]> remoteFileSupplier;
	private final String className;

	public ProvidedJavaFileObject(String name, JavaFileObject.Kind kind, Supplier<byte[]> remoteFileSupplier) {
		super(URI.create("provided:///" + name.replace('.', '/') + kind.extension), kind);
		this.className = name;
		this.remoteFileSupplier = remoteFileSupplier;
	}

	@Override
	public InputStream openInputStream() {
		final byte[] bytes = remoteFileSupplier.get();
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public String toString() {
		return className;
	}

}
