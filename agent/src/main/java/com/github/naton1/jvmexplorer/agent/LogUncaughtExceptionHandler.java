package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.minlog.Log;

public class LogUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		Log.error("Client update thread threw exception", e);
	}

}
