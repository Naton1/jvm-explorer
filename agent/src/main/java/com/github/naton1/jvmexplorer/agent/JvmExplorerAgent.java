package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.AgentConfiguration;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class JvmExplorerAgent {

	public static void agentmain(String agentArgs, Instrumentation instrumentation) {
		final AgentConfiguration agentConfiguration = AgentConfiguration.parseAgentArgs(agentArgs);
		final ScheduledExecutorService executorService = createExecutorService();
		final AgentFileLogger logger = setupLogger(agentConfiguration.getLogFilePath(),
		                                           agentConfiguration.getLogLevel());
		Log.info("Agent connected. Configuration: " + agentConfiguration);
		try {
			final ClientLauncher clientLauncher = new ClientLauncher();
			clientLauncher.launch(executorService, agentConfiguration, instrumentation, logger);
		}
		catch (Exception e) {
			Log.error("Agent setup failed", e);
			logger.close();
			executorService.shutdown();
		}
	}

	private static ScheduledExecutorService createExecutorService() {
		final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3,
		                                                                                           new LogUncaughtExceptionThreadFactory());
		return new VerboseScheduledExecutorService(scheduledExecutorService);
	}

	private static AgentFileLogger setupLogger(String logFilePath, int logLevel) {
		final AgentFileLogger logger = new AgentFileLogger(null, new File(logFilePath), true);
		Log.setLogger(logger);
		Log.set(logLevel);
		Log.info("Initialized logger");
		return logger;
	}

}
