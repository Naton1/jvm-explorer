package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarFile;

@Slf4j
public class PatchHelper {

	public boolean patch(File jarFile, RunningJvm runningJvm, ClientHandler clientHandler,
	                     Consumer<Integer> patchedClasses) {
		log.debug("Attempting to patch {} with {}", runningJvm, jarFile);
		final AtomicInteger patchedClassCount = new AtomicInteger();
		try (final JarFile jar = new JarFile(jarFile)) {
			jar.stream().parallel().filter(j -> j.getName().endsWith(".class")).forEach(classFile -> {
				try {
					final String name = classFile.getName().replace('/', '.').replace(".class", "");
					log.debug("Patching {}", name);
					final byte[] classContents = jar.getInputStream(classFile).readAllBytes();
					final boolean replaced = clientHandler.replaceClass(runningJvm, name, classContents);
					if (!replaced) {
						throw new IllegalStateException("Failed to replace class on jvm: " + name);
					}
				}
				catch (IOException e) {
					log.warn("Failed to process {}", classFile.getName());
					throw new UncheckedIOException(e);
				}
				patchedClasses.accept(patchedClassCount.incrementAndGet());
			});
			return true;
		}
		catch (IOException | UncheckedIOException | IllegalStateException e) {
			log.warn("Failed to patch {} with {}", runningJvm, jarFile, e);
			return false;
		}
	}

}
