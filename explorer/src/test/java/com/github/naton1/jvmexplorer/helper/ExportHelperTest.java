package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class ExportHelperTest {

	private static final RunningJvm JVM = new RunningJvm("id", "name");

	@Mock
	private ClientHandler clientHandler;

	@Test
	void testExport() throws IOException {
		final ExportHelper exportHelper = new ExportHelper(clientHandler);
		final ClassLoaderDescriptor classLoaderDescriptor = ClassLoaderDescriptor.builder().build();
		final List<LoadedClass> classesToExport = List.of(new LoadedClass("org.test.MyClass", null, null),
		                                                  new LoadedClass("org.othertest.SomeClass",
		                                                                  classLoaderDescriptor,
		                                                                  null),
		                                                  new LoadedClass("testing.Export",
		                                                                  classLoaderDescriptor,
		                                                                  null));

		final Map<String, byte[]> classData = classesToExport.stream()
		                                                     .collect(Collectors.toMap(LoadedClass::getName,
		                                                                               l -> generateClassBytes()));

		Mockito.when(clientHandler.getClassBytes(ArgumentMatchers.eq(JVM), ArgumentMatchers.any()))
		       .thenAnswer(ctx -> classData.get(ctx.getArgument(1, LoadedClass.class).getName()));

		final File outputFile = File.createTempFile("export", ".jar");
		final AtomicInteger exportCount = new AtomicInteger();

		final boolean success = exportHelper.export(JVM, classesToExport, outputFile, exportCount::set);

		Assertions.assertTrue(success);
		Assertions.assertEquals(classesToExport.size(), exportCount.get());

		final JarFile jarFile = new JarFile(outputFile);
		final List<JarEntry> jarEntries = jarFile.stream()
		                                         .filter(entry -> entry.getName().endsWith(".class"))
		                                         .collect(Collectors.toList());
		Assertions.assertEquals(jarEntries.size(), classesToExport.size());

		for (JarEntry jarEntry : jarEntries) {
			final byte[] jarClassFile = jarFile.getInputStream(jarEntry).readAllBytes();
			final byte[] baseClassFile = classData.get(jarEntry.getName().replace('/', '.').replace(".class", ""));
			Assertions.assertArrayEquals(baseClassFile, jarClassFile);
		}
	}

	private byte[] generateClassBytes() {
		// These are of course not real class files, but random byte arrays to ensure exporting works
		final int size = ThreadLocalRandom.current().nextInt(100) + 20;
		final byte[] classFile = new byte[size];
		ThreadLocalRandom.current().nextBytes(classFile);
		return classFile;
	}

}