package com.github.naton1.jvmexplorer.net;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.Protocol;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class ServerLauncher {

	public Server launch(ScheduledExecutorService executorService, ClientHandler clientHandler) {
		final int minlogLevel = getLogLevel();
		log.debug("Setting minlog log level to {}", minlogLevel);
		Log.set(minlogLevel);
		Log.setLogger(new MinlogToSl4fj());
		final Server server = new Server(1000000, 1000000) {
			protected Connection newConnection() {
				return new JvmClientImpl(executorService);
			}
		};
		final Kryo kryo = server.getKryo();
		Protocol.register(kryo);
		server.addListener(clientHandler);
		executorService.submit(() -> {
			server.start();
			try {
				server.bind(Protocol.PORT);
			}
			catch (IOException e) {
				log.error("Failed to bind server to port {}", Protocol.PORT, e);
			}
		});
		return server;
	}

	private int getLogLevel() {
		if (log.isTraceEnabled()) {
			return Log.LEVEL_TRACE;
		}
		else if (log.isDebugEnabled()) {
			return Log.LEVEL_DEBUG;
		}
		else if (log.isInfoEnabled()) {
			return Log.LEVEL_INFO;
		}
		else if (log.isWarnEnabled()) {
			return Log.LEVEL_WARN;
		}
		else if (log.isErrorEnabled()) {
			return Log.LEVEL_ERROR;
		}
		else {
			return Log.LEVEL_NONE;
		}
	}

}
