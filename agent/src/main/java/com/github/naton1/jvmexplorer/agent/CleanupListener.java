package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class CleanupListener extends Listener {

	private final ExecutorService executorService;
	private final AgentFileLogger agentFileLogger;

	@Override
	public void disconnected(Connection connection) {
		Log.info("Cleaning up");
		executorService.shutdown();
		agentFileLogger.close();
	}

}
