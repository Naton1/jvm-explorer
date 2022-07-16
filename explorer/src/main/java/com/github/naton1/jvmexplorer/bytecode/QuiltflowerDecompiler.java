package com.github.naton1.jvmexplorer.bytecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.util.jar.Manifest;

@Slf4j
public class QuiltflowerDecompiler implements Decompiler {

	public String process(byte[] bytes) {
		final IBytecodeProvider bytecodeProvider = new BytecodeProvider(bytes);
		final ResultSaver resultSaver = new ResultSaver();
		final IFernflowerLogger fernflowerLogger = new FernflowerLogger();
		final Fernflower fernflower = new Fernflower(bytecodeProvider,
		                                             resultSaver,
		                                             IFernflowerPreferences.DEFAULTS,
		                                             fernflowerLogger);

		try {
			fernflower.addSource(new File("fake-file.class"));
			fernflower.decompileContext();
		}
		catch (Exception e) {
			log.warn("Failed to decompile class", e);
		}

		return resultSaver.getContent();
	}

	@RequiredArgsConstructor
	private static class BytecodeProvider implements IBytecodeProvider {
		private final byte[] bytes;

		@Override
		public byte[] getBytecode(String externalPath, String internalPath) {
			return bytes;
		}
	}

	private static class FernflowerLogger extends IFernflowerLogger {
		@Override
		public void writeMessage(String message, Severity severity) {
			switch (severity) {
			case TRACE:
				log.trace(message);
				break;
			case INFO:
				log.info(message);
				break;
			case WARN:
				log.warn(message);
				break;
			case ERROR:
				log.error(message);
				break;
			}
		}

		@Override
		public void writeMessage(String message, Severity severity, Throwable t) {
			switch (severity) {
			case TRACE:
				log.trace(message, t);
				break;
			case INFO:
				log.info(message, t);
				break;
			case WARN:
				log.warn(message, t);
				break;
			case ERROR:
				log.error(message, t);
				break;
			}
		}
	}

	private static class ResultSaver implements IResultSaver {
		@Getter
		private String content;

		@Override
		public void saveFolder(String path) {}

		@Override
		public void copyFile(String source, String path, String entryName) {}

		@Override
		public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
			this.content = content;
		}

		@Override
		public void createArchive(String path, String archiveName, Manifest manifest) {}

		@Override
		public void saveDirEntry(String path, String archiveName, String entryName) {}

		@Override
		public void copyEntry(String source, String path, String archiveName, String entry) {}

		@Override
		public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName,
		                           String content) {}

		@Override
		public void closeArchive(String path, String archiveName) {}
	}

}
