package com.github.naton1.jvmexplorer.agent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CleanClassLoaderStore implements Runnable {

	private final ClassLoaderStore classLoaderStore;

	@Override
	public void run() {
		classLoaderStore.clean();
	}

}
