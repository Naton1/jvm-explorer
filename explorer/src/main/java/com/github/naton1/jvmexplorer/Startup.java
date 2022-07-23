package com.github.naton1.jvmexplorer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Startup {

	public static void main(String[] args) {
		final String version = Startup.class.getPackage().getImplementationVersion();
		log.info("Starting application. Application Version: {}. Java Version: {}.",
		         (version == null ? "Development" : version),
		         System.getProperty("java.version"));
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.warn("Thread uncaught exception: " + t, e);
		});
		JvmExplorer.launch(JvmExplorer.class, args);
	}

}
