package com.github.naton1.jvmexplorer.bytecode.compile;

import lombok.extern.slf4j.Slf4j;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class ProvidedJvmFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

	private final JavacBytecodeProvider javacBytecodeProvider;

	private OutputJavaFileObject output;

	public ProvidedJvmFileManager(StandardJavaFileManager fileManager, JavacBytecodeProvider javacBytecodeProvider) {
		super(fileManager);
		this.javacBytecodeProvider = javacBytecodeProvider;
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
	                                     boolean recurse) throws IOException {
		final Iterable<JavaFileObject> standardJavaFileObjects = fileManager.list(location,
		                                                                          packageName,
		                                                                          kinds,
		                                                                          recurse);
		if (location != StandardLocation.CLASS_PATH || !kinds.contains(JavaFileObject.Kind.CLASS)) {
			return standardJavaFileObjects;
		}
		final List<JavaFileObject> remoteClasses = javacBytecodeProvider.list(packageName, recurse);
		log.debug("Loaded package {} in provided classpath: {}", packageName, remoteClasses);

		final List<JavaFileObject> results = new ArrayList<>();
		for (JavaFileObject javaFileObject : standardJavaFileObjects) {
			results.add(javaFileObject);
		}
		results.addAll(remoteClasses);

		return results;
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof ProvidedJavaFileObject) {
			return file.toString();
		}
		return super.inferBinaryName(location, file);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className,
	                                           JavaFileObject.Kind kind, FileObject sibling) {
		return this.output = new OutputJavaFileObject(className, kind);
	}

	public byte[] getOutputClassContent() {
		if (output == null) {
			return null;
		}
		return output.getBytes();
	}

}
