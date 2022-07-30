package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import com.esotericsoftware.kryonet.rmi.RemoteObject;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.AgentConfiguration;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import com.github.naton1.jvmexplorer.protocol.Protocol;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientLauncher {

	public Client launch(ScheduledExecutorService executorService, AgentConfiguration agentConfiguration,
	                   Instrumentation instrumentation, AgentFileLogger logger) throws IOException {
		final Client client = new Client(Protocol.WRITE_BUFFER_SIZE, Protocol.OBJECT_BUFFER_SIZE);
		setupRmi(client, executorService, agentConfiguration.getIdentifier(), instrumentation);
		startClient(client, agentConfiguration.getHostName(), agentConfiguration.getPort());
		client.addListener(new CleanupListener(executorService, logger));
		Log.info("Client connected");
		return client;
	}

	private static void setupRmi(Client client, ScheduledExecutorService executorService, String identifier,
	                             Instrumentation instrumentation) {
		final Kryo kryo = client.getKryo();
		Protocol.register(kryo);
		final JvmClient serverTracker = ObjectSpace.getRemoteObject(client, Protocol.RMI_JVM_CLIENT, JvmClient.class);
		((RemoteObject) serverTracker).setNonBlocking(true);
		((RemoteObject) serverTracker).setTransmitReturnValue(false);
		((RemoteObject) serverTracker).setTransmitExceptions(false);
		final InstrumentationHelper instrumentationHelper = new InstrumentationHelper(instrumentation);
		final ClassLoaderStore classLoaderStore = new ClassLoaderStore();
		executorService.scheduleWithFixedDelay(new CleanClassLoaderStore(classLoaderStore), 10, 10, TimeUnit.SECONDS);
		final JvmConnectionImpl jvmConnectionImpl = new JvmConnectionImpl(serverTracker,
		                                                                  instrumentationHelper,
		                                                                  client,
		                                                                  executorService,
		                                                                  classLoaderStore);
		final ObjectSpace objectSpace = new ObjectSpace(client);
		objectSpace.register(Protocol.RMI_JVM_CONNECTION, jvmConnectionImpl);
		final ClientListener clientListener = new ClientListener(executorService, identifier, serverTracker);
		objectSpace.setExecutor(executorService);
		client.addListener(clientListener);
	}

	private static void startClient(Client client, String host, int port) throws IOException {
		client.start();
		client.connect(15000, host, port);
		client.getUpdateThread().setUncaughtExceptionHandler(new LogUncaughtExceptionHandler());
	}

}
