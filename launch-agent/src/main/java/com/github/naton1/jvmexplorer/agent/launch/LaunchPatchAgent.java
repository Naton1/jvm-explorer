package com.github.naton1.jvmexplorer.agent.launch;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class LaunchPatchAgent {

	public static void premain(String agentArgs, Instrumentation instrumentation) {
		main(agentArgs, instrumentation);
	}

	private static void main(String agentArgs, Instrumentation instrumentation) {
		final ClassFileTransformer transformer = new LaunchPatchClassFileTransformer();
		instrumentation.addTransformer(transformer, true);
		try {
			instrumentation.retransformClasses(ProcessBuilder.class);
		}
		catch (UnmodifiableClassException e) {
			e.printStackTrace();
		}
	}

	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		main(agentArgs, instrumentation);
	}

}
