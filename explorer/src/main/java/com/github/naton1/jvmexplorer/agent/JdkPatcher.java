package com.github.naton1.jvmexplorer.agent;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

@Slf4j
class JdkPatcher {

	static boolean patchJdkForAgent(RunningJvm runningJvm) {
		try {
			if (!System.getProperty("os.name").toLowerCase().contains("win")) {
				// Patch only supports windows at the moment
				return false;
			}
			final Properties properties = runningJvm.getSystemProperties();
			final String javaHome = properties.getProperty("java.home");
			log.debug("Patching {}", javaHome);
			final File instrumentFile = new File(javaHome, "bin" + File.separator + "instrument.dll");
			if (instrumentFile.exists()) {
				// Already exists, or is patched already
				return false;
			}
			try (final FileOutputStream fileOutputStream = new FileOutputStream(instrumentFile);
			     final InputStream inputStream = Objects.requireNonNull(JdkPatcher.class.getClassLoader()
			                                                                            .getResource("jdk_patch"
			                                                                                         + "/instrument"
			                                                                                         + ".dll"))
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
