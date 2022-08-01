package com.github.naton1.jvmexplorer.integration.programs;

public class SleepForeverProgram {

	// This is used for integration tests to verify fields are found
	private static int someField = 15;

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Sleeping...");
		Thread.sleep(60000L);
		// We don't actually want to sleep forever, to ensure this process doesn't accidentally hang around
	}

}