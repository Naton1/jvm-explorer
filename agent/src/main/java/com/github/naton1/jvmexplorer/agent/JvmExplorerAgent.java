package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import com.esotericsoftware.kryonet.rmi.RemoteObject;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.AgentConfiguration;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import com.github.naton1.jvmexplorer.protocol.Protocol;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JvmExplorerAgent {

	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		final AgentConfiguration agentConfiguration = AgentConfiguration.parseAgentArgs(agentArgs);
		final ExecutorService executorService = createExecutorService();
		final AgentFileLogger logger = setupLogger(agentConfiguration.getLogFilePath(),
		                                           agentConfiguration.getLogLevel());
		Log.info("Agent connected. Configuration: " + agentConfiguration);
		try {
			final Client client = new Client(1000000, 1000000);
			setupRmi(client, executorService, agentConfiguration.getIdentifier(), instrumentation);
			startClient(client, agentConfiguration.getHostName(), agentConfiguration.getPort());
			client.addListener(new CleanupListener(executorService, logger));
			Log.info("Client connected");
		}
		catch (Exception e) {
			Log.error("Agent setup failed", e);
			logger.close();
			executorService.shutdown();
		}
	}

	private static ExecutorService createExecutorService() {
		return Executors.newFixedThreadPool(3, new LogUncaughtExceptionThreadFactory());
	}

	private static AgentFileLogger setupLogger(String logFilePath, int logLevel) {
		final AgentFileLogger logger = new AgentFileLogger(null, new File(logFilePath), true);
		Log.setLogger(logger);
		Log.set(logLevel);
		return logger;
	}

	private static void setupRmi(Client client, ExecutorService executorService, String identifier,
	                             Instrumentation instrumentation) {
		final Kryo kryo = client.getKryo();
		Protocol.register(kryo);
		final JvmClient serverTracker = ObjectSpace.getRemoteObject(client, Protocol.RMI_JVM_CLIENT, JvmClient.class);
		((RemoteObject) serverTracker).setNonBlocking(true);
		((RemoteObject) serverTracker).setTransmitReturnValue(false);
		((RemoteObject) serverTracker).setTransmitExceptions(false);
		final InstrumentationHelper instrumentationHelper = new InstrumentationHelper(instrumentation);
		final JvmConnectionImpl jvmConnectionImpl = new JvmConnectionImpl(serverTracker,
		                                                                  instrumentationHelper,
		                                                                  client,
		                                                                  executorService);
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
