package com.github.naton1.jvmexplorer;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class Startup {

	private static final File AGENT_LOG_FILE = new File(System.getProperty("user.home"),
	                                                    "jvm-explorer/logs/agent" + ".log");

	public static void main(String[] args) {
		final String version = Startup.class.getPackage().getImplementationVersion();
		log.info("Starting application. Application Version: {}. Java Version: {}.",
		         (version == null ? "Development" : version),
		         System.getProperty("java.version"));
		AGENT_LOG_FILE.delete(); // Delete agent on log on start to prevent it growing too large
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.warn("Thread uncaught exception: " + t, e);
		});
		JvmExplorer.launch(JvmExplorer.class, args);
	}

}
