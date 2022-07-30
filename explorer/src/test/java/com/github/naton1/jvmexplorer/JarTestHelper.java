package com.github.naton1.jvmexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class JarTestHelper {

	public static File buildJar(Class<?> sourceClass) throws Exception {
		final URL base = sourceClass.getProtectionDomain().getCodeSource().getLocation();

		final String classFileName = sourceClass.getName().replace('.', '/') + ".class";
		final URI classFileUri = base.toURI().resolve(classFileName);
		final byte[] classFile = classFileUri.toURL().openStream().readAllBytes();
		final File tempJar = File.createTempFile("test", ".jar");

		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
		manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, sourceClass.getName());

		final JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tempJar), manifest);

		jarOutputStream.putNextEntry(new ZipEntry(classFileName));
		jarOutputStream.write(classFile);
		jarOutputStream.closeEntry();

		jarOutputStream.close();

		return tempJar;
	}

}
