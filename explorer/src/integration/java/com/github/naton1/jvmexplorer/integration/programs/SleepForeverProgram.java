package com.github.naton1.jvmexplorer.integration.programs;

import java.util.ArrayList;
import java.util.List;

public class SleepForeverProgram {

	// This is used for integration tests to verify fields are found
	private static int someField = 15;

	private static final List<String> complexField = new ArrayList<>();

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Sleeping...");
		Thread.sleep(60000L);
		// We don't actually want to sleep forever, to ensure this process doesn't accidentally hang around
	}

	// This is used for remote code execution test
	public static String testFunction(String arg) {
		return arg;
	}

}