package com.github.naton1.jvmexplorer.net;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

class OpenPortProviderTest {

	@Test
	void testPortIsOpen() throws IOException {
		final OpenPortProvider openPortProvider = new OpenPortProvider();
		final int openPort = openPortProvider.getOpenPort();
		try (final ServerSocket serverSocket = new ServerSocket(openPort)) {
			serverSocket.setReuseAddress(true);
			// No exception thrown

			// This is prone to a rare race condition if another process steals the port.
			// The test could fail even if it's not taken. It's okay here because it's so rare.
		}
	}

}