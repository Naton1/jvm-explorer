package com.github.naton1.jvmexplorer.bytecode.compile;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;

public class InputJavaFileObject extends SimpleJavaFileObject {

	private final CharSequence content;

	public InputJavaFileObject(String className, CharSequence content) {
		super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
		      JavaFileObject.Kind.SOURCE);
		this.content = content;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return this.content;
	}

}
