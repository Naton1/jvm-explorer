package com.github.naton1.jvmexplorer.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.ServerSocket;
import java.util.concurrent.ScheduledExecutorService;

class ServerLauncherTest {

	@Test
	void givenValidOpenPort_whenLaunch_thenServerBoundToPort() throws Exception {
		final OpenPortProvider openPortProvider = Mockito.mock(OpenPortProvider.class);
		try (final ServerSocket serverSocket = new ServerSocket(0)) {
			serverSocket.setReuseAddress(true);
			final int port = serverSocket.getLocalPort();
			Mockito.when(openPortProvider.getOpenPort()).thenAnswer(invocation -> {
				serverSocket.close();
				return port;
			});
			final ServerLauncher serverLauncher = new ServerLauncher(openPortProvider);

			final ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);
			final ClientHandler clientHandler = Mockito.mock(ClientHandler.class);

			final JvmExplorerServer jvmExplorerServer = serverLauncher.launch(executorService, clientHandler);
			try {
				Assertions.assertNotEquals(0, jvmExplorerServer.getPort());
				Assertions.assertEquals(port, jvmExplorerServer.getPort());
			}
			finally {
				jvmExplorerServer.close();
			}
		}
	}

	@Test
	void givenInitialPortTaken_whenLaunch_thenServerFindsAnotherPortToBind() throws Exception {
		final OpenPortProvider openPortProvider = Mockito.mock(OpenPortProvider.class);
		try (final ServerSocket serverSocket = new ServerSocket(0)) {
			serverSocket.setReuseAddress(true);
			final int port = serverSocket.getLocalPort();
			Mockito.when(openPortProvider.getOpenPort())
			       .thenReturn(port) // Give taken port a few times
			       .thenReturn(port)
			       .thenReturn(port)
			       .thenAnswer(invocation -> {
				       serverSocket.close(); // Release port, and return it again
				       return port;
			       });
			final ServerLauncher serverLauncher = new ServerLauncher(openPortProvider);

			final ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);
			final ClientHandler clientHandler = Mockito.mock(ClientHandler.class);

			final JvmExplorerServer jvmExplorerServer = serverLauncher.launch(executorService, clientHandler);
			try {
				Assertions.assertNotEquals(0, jvmExplorerServer.getPort());
				Assertions.assertEquals(port, jvmExplorerServer.getPort());
			}
			finally {
				jvmExplorerServer.close();
			}
		}
	}

}