package com.github.naton1.jvmexplorer.bytecode.compile;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class RemoteJavacBytecodeProvider implements JavacBytecodeProvider {

	private final ClientHandler clientHandler;
	private final RunningJvm runningJvm;

	private final List<LoadedClass> classpath;

	@Override
	public List<JavaFileObject> list(String packageName, boolean recurse) {
		return classpath.stream()
		                .filter(l -> isClassInPackage(l, packageName, recurse))
		                .map(this::getProvidedJavaFileObject)
		                .collect(Collectors.toList());
	}

	private boolean isClassInPackage(LoadedClass loadedClass, String packageName, boolean recurse) {
		final String classPackageName = ClassNameHelper.getPackageName(loadedClass.getName());
		if (recurse && classPackageName.startsWith(packageName + ".")) {
			return true;
		}
		return classPackageName.equals(packageName);
	}

	private ProvidedJavaFileObject getProvidedJavaFileObject(LoadedClass loadedClass) {
		return new ProvidedJavaFileObject(loadedClass.getName(),
		                                  JavaFileObject.Kind.CLASS,
		                                  () -> clientHandler.getClassBytes(runningJvm, loadedClass));
	}

}
