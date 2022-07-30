package com.github.naton1.jvmexplorer.integration;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.agent.AgentFileLogger;
import com.github.naton1.jvmexplorer.agent.ClientLauncher;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.net.JvmExplorerServer;
import com.github.naton1.jvmexplorer.net.OpenPortProvider;
import com.github.naton1.jvmexplorer.net.ServerLauncher;
import com.github.naton1.jvmexplorer.protocol.AgentConfiguration;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ClientServerCommunicationTest {

	private static final RunningJvm RUNNING_JVM = new RunningJvm("1000", "TestJvm");

	private final Set<RunningJvm> connected = ConcurrentHashMap.newKeySet();

	private ScheduledExecutorService serverExecutor;
	private ClientHandler clientHandler;
	private JvmExplorerServer jvmExplorerServer;

	private ScheduledExecutorService clientExecutor;
	private Instrumentation instrumentation;
	private Client client;

	@BeforeEach
	void setup() throws Exception {
		serverExecutor = Executors.newScheduledThreadPool(3);
		clientHandler = ClientHandler.builder()
		                             .onConnect(((runningJvm, connection) -> connected.add(runningJvm)))
		                             .onDisconnect(connected::remove)
		                             .build();
		jvmExplorerServer = new ServerLauncher(new OpenPortProvider()).launch(serverExecutor, clientHandler);

		final File tmp = File.createTempFile("agent", ".log");

		final AgentConfiguration agentConfiguration = AgentConfiguration.builder()
		                                                                .hostName("localhost")
		                                                                .identifier(RUNNING_JVM.toIdentifier())
		                                                                .logLevel(Log.LEVEL_DEBUG)
		                                                                .port(jvmExplorerServer.getPort())
		                                                                .logFilePath(tmp.getAbsolutePath())
		                                                                .build();

		clientExecutor = Executors.newScheduledThreadPool(3);

		instrumentation = Mockito.mock(Instrumentation.class);
		final AgentFileLogger agentFileLogger = new AgentFileLogger(clientExecutor, tmp, false);

		client = new ClientLauncher().launch(clientExecutor, agentConfiguration, instrumentation, agentFileLogger);

		WaitForAsyncUtils.waitFor(3000, TimeUnit.MILLISECONDS, () -> connected.contains(RUNNING_JVM));
	}

	@AfterEach
	void teardown() {
		jvmExplorerServer.close();
		client.close();

		serverExecutor.shutdown();
		clientExecutor.shutdown();

		connected.clear();
	}

	@Test
	void testConnect() {
		Assertions.assertTrue(connected.contains(RUNNING_JVM));
	}

	@Test
	void testListClasses() {

		Mockito.when(instrumentation.getAllLoadedClasses())
		       .thenReturn(new Class[] { Integer.class, int.class, String.class,
		                                 ClientServerCommunicationTest.class });

		Mockito.when(instrumentation.isModifiableClass(ArgumentMatchers.any()))
				.thenReturn(true);

		final AtomicInteger loaded = new AtomicInteger();
		final List<LoadedClass> loadedClasses = clientHandler.getLoadedClasses(RUNNING_JVM, loaded::set);

		Assertions.assertNotNull(loadedClasses);
		Assertions.assertEquals(loaded.get(), loadedClasses.size());
		Assertions.assertEquals(3, loadedClasses.size());
	}

}
