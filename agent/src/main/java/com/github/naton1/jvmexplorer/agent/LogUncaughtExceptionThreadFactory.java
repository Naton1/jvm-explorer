package com.github.naton1.jvmexplorer.agent;

import java.util.concurrent.ThreadFactory;

public class LogUncaughtExceptionThreadFactory implements ThreadFactory {

	@Override
	public Thread newThread(Runnable r) {
		final Thread newThread = new Thread(r);
		newThread.setUncaughtExceptionHandler(new LogUncaughtExceptionHandler());
		return newThread;
	}

}
