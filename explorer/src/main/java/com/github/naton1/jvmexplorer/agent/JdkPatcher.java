package com.github.naton1.jvmexplorer.agent;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

@Slf4j
class JdkPatcher {

	private static final String INSTRUMENT_32_BIT = "jdk_patch/instrument-32.dll";
	private static final String INSTRUMENT_64_BIT = "jdk_patch/instrument-64.dll";

	/**
	 * Attempts to patch the target jvm to support attaching. This may only work in a few cases.
	 *
	 * @param runningJvm
	 * 		the jvm to patch
	 * @return true if a patch was applied and a re-attach should be tried, false if nothing was changed
	 */
	static boolean patchJdkForAgent(RunningJvm runningJvm) {
		try {
			if (!System.getProperty("os.name").toLowerCase().contains("win")) {
				// Patch only supports windows at the moment
				return false;
			}
			final Properties properties = runningJvm.getSystemProperties();
			final String javaHome = properties.getProperty("java.home");
			final boolean is32Bit = "x86".equals(properties.getProperty("os.arch"));
			final String resourceName = is32Bit ? INSTRUMENT_32_BIT : INSTRUMENT_64_BIT;
			log.debug("Patching {}", javaHome);
			final File instrumentFile = new File(javaHome, "bin" + File.separator + "instrument.dll");
			if (instrumentFile.exists()) {
				log.debug("Resource already exists for {}", javaHome);
				// Already exists, or is patched already
				return false;
			}
			try (final FileOutputStream fileOutputStream = new FileOutputStream(instrumentFile);
			     final InputStream inputStream = Objects.requireNonNull(JdkPatcher.class.getClassLoader()
			                                                                            .getResource(resourceName))
			                                            .openStream()
			) {
				inputStream.transferTo(fileOutputStream);
			}
			log.debug("Patched {}", instrumentFile);
			return true;
		}
		catch (Exception e) {
			log.debug("Failed to patch agent", e);
			return false;
		}
	}

}
