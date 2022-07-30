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
		                .filter(l -> {
			                final String classPackageName = ClassNameHelper.getPackageName(l.getName());
			                if (recurse) {
				                return classPackageName.startsWith(packageName);
			                }
			                return classPackageName.equals(packageName);
		                })
		                .map(loadedClass -> new ProvidedJavaFileObject(loadedClass.getName(),
		                                                               JavaFileObject.Kind.CLASS,
		                                                               () -> clientHandler.getClassBytes(runningJvm,
		                                                                                                 loadedClass)))
		                .collect(Collectors.toList());
	}

}
