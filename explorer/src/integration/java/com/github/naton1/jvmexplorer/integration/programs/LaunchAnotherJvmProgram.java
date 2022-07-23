package com.github.naton1.jvmexplorer.integration.programs;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;

public class LaunchAnotherJvmProgram {

	public static void main(String[] args) throws Exception {
		final long pid = TestJvm.builder()
		                        .sourceClass(SleepForeverProgram.class)
		                        .jvmArg("-XX:+DisableAttachMechanism")
		                        .build().getProcess().pid();
		System.out.println(pid);
	}

}
