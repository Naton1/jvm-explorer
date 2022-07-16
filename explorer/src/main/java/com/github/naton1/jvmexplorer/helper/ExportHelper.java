package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

@RequiredArgsConstructor
@Slf4j
public class ExportHelper {

	private final ClientHandler clientHandler;

	public boolean export(RunningJvm jvm, List<String> classNames, File outputJar, Consumer<Integer> currentProgress) {
		log.debug("Exporting {} files in {} to {}", classNames.size(), jvm, outputJar);
		try {
			Files.deleteIfExists(outputJar.toPath());
			Files.createFile(outputJar.toPath());
		}
		catch (IOException e) {
			log.warn("Failed to create initial file for export", e);
			return false;
		}
		final AtomicInteger count = new AtomicInteger();
		try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(outputJar.toPath()))) {
			// Note - parallel stream runs in common fork join pool despite these being io bound tasks
			classNames.stream()
			          .parallel()
			          .map(name -> new Pair<>(name, clientHandler.getExportFile(jvm, name)))
			          .forEach(pair -> {
				          currentProgress.accept(count.incrementAndGet());
				          final String name = pair.getKey().replace('.', '/') + ".class";
				          final byte[] content = pair.getValue();
				          write(name, content, jarOutputStream);
			          });
			log.debug("Jar created: {} with {} classes", outputJar, count.get());
			return true;
		}
		catch (IOException | UncheckedIOException e) {
			log.warn("Failed to export", e);
			return false;
		}
	}

	private void write(String name, byte[] content, JarOutputStream jarOutputStream) {
		final ZipEntry zipEntry = new ZipEntry(name);
		try {
			// This synchronization is intentional and it works. Ignore any IDE warning.
			synchronized (jarOutputStream) {
				jarOutputStream.putNextEntry(zipEntry);
				jarOutputStream.write(content);
				jarOutputStream.closeEntry();
			}
		}
		catch (IOException e) {
			log.warn("Failed to write zip file: {}", name, e);
			throw new UncheckedIOException(e);
		}
	}

}
