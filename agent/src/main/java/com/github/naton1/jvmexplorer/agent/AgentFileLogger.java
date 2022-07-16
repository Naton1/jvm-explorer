package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.minlog.Log;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class AgentFileLogger extends Log.Logger {

	private final ExecutorService executorService;

	private final File outputFile;
	private final boolean append;

	private volatile PrintWriter printWriter;
	private volatile boolean closed = false;

	public void close() {
		closed = true;
		final PrintWriter printWriter = this.printWriter;
		if (printWriter != null) {
			printWriter.close();
		}
	}

	@Override
	protected void print(String message) {
		if (closed) {
			System.out.println(message);
			return;
		}
		if (executorService == null) {
			printMessage(message);
			return;
		}
		executorService.submit(new Print(message));
	}

	private void printMessage(String message) {
		try {
			getPrintWriter().println(message);
		}
		catch (IOException e) {
			// This should ideally never happen...
			super.print(message);
		}
	}

	private PrintWriter getPrintWriter() throws IOException {
		if (printWriter == null) {
			synchronized (this) {
				if (printWriter == null) {
					outputFile.getParentFile().mkdirs();
					final FileWriter fileWriter = new FileWriter(outputFile, append);
					printWriter = new PrintWriter(fileWriter);
				}
			}
		}
		return printWriter;
	}

	@RequiredArgsConstructor
	private class Print implements Runnable {
		private final String message;

		@Override
		public void run() {
			AgentFileLogger.this.printMessage(message);
		}
	}

}
