package com.github.naton1.jvmexplorer.bytecode.compile;

import lombok.extern.slf4j.Slf4j;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class Compiler {

	public CompileResult compile(int targetVersion, String className, String classContent,
	                             JavacBytecodeProvider bytecodeProvider) {

		log.debug("Compiling class {} (source version: {}): {}", className, targetVersion, classContent);

		try {

			final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

			final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

			final StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(null, null, null);
			final ProvidedJvmFileManager providedJvmFileManager = new ProvidedJvmFileManager(standardFileManager,
			                                                                                 bytecodeProvider);
			final StringWriter out = new StringWriter();

			final List<InputJavaFileObject> files = new ArrayList<>();
			files.add(new InputJavaFileObject(className, classContent));

			final List<String> compilerOptions = List.of("-source",
			                                             Integer.toString(targetVersion),
			                                             "-target",
			                                             Integer.toString(targetVersion));

			final Callable<Boolean> compileTask = compiler.getTask(out,
			                                                       providedJvmFileManager,
			                                                       diagnostics,
			                                                       compilerOptions,
			                                                       null,
			                                                       files);

			final boolean result = compileTask.call();

			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
				out.append(diagnostic.toString()).append(System.lineSeparator());
			}

			final String output = out.toString();

			log.debug("Compiled class: {} - {}", result, output);

			return CompileResult.builder()
			                    .stdOut(output)
			                    .success(result)
			                    .classContent(providedJvmFileManager.getOutputClassContent())
			                    .build();
		}
		catch (Exception e) {
			log.warn("Caught exception while compiling", e);
			return CompileResult.builder().success(false).stdOut(e.getMessage()).build();
		}
	}

}
