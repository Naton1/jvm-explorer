package com.github.naton1.jvmexplorer.integration.helper;

import java.util.function.Supplier;

public class TestHelper {

	public static <T> T waitFor(Supplier<T> supplier, long timeoutMs) throws InterruptedException {
		final long end = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < end) {
			final T result = supplier.get();
			if (result != null) {
				return result;
			}
			Thread.sleep(10);
		}
		throw new IllegalStateException("No result found after " + timeoutMs + " ms");
	}

}
