package com.github.naton1.jvmexplorer.bytecode;

import org.openjdk.asmtools.Main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

class OpenJdkTools {

	private static File outputLocation;

	static synchronized String getJarLocation() throws IOException {
		if (outputLocation == null) {
			outputLocation = prepare();
		}
		return outputLocation.getAbsolutePath();
	}

	private static File prepare() throws IOException {
		final URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
		final byte[] jar = location.openStream().readAllBytes();
		final File tmpFile = File.createTempFile("openjdk-code-tools", ".jar");
		Files.write(tmpFile.toPath(), jar);
		return tmpFile;
	}

}
