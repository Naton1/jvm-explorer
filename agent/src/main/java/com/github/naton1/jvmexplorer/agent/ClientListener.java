package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class ClientListener extends Listener {

	private final ExecutorService executorService;
	private final String identifier;
	private final JvmClient jvmClient;

	@Override
	public void connected(Connection connection) {
		executorService.submit(new Register(jvmClient, identifier));
	}

	@RequiredArgsConstructor
	private static class Register implements Runnable {
		private final JvmClient jvmClient;
		private final String identifier;

		@Override
		public void run() {
			jvmClient.register(identifier);
			Log.info("Registered client");
		}
	}

}
