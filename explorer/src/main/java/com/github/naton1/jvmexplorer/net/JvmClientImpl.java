package com.github.naton1.jvmexplorer.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import com.esotericsoftware.kryonet.rmi.RemoteObject;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import com.github.naton1.jvmexplorer.protocol.JvmConnection;
import com.github.naton1.jvmexplorer.protocol.Protocol;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
public class JvmClientImpl extends Connection implements JvmClient {

	private final Map<Integer, PacketResponseHandler<?>> packetResponseHandlers = new ConcurrentHashMap<>();
	private final ScheduledExecutorService executorService;

	@Getter
	private final JvmConnection jvmConnection;

	@Setter
	private volatile Consumer<RunningJvm> onRegister;

	@Getter
	private volatile RunningJvm runningJvm;

	public JvmClientImpl(ScheduledExecutorService executorService) {
		this.executorService = executorService;
		final ObjectSpace objectSpace = new ObjectSpace(this);
		objectSpace.setExecutor(executorService);
		objectSpace.register(Protocol.RMI_JVM_CLIENT, this);
		jvmConnection = ObjectSpace.getRemoteObject(this, Protocol.RMI_JVM_CONNECTION, JvmConnection.class);
		// Loading class names can take a bit
		((RemoteObject) jvmConnection).setResponseTimeout(30000);
		((RemoteObject) jvmConnection).setTransmitExceptions(false);
	}

	@Override
	public void register(String identifier) {
		if (this.runningJvm != null) {
			close();
			return;
		}
		final String[] id = identifier.split(":", 2);
		if (id.length != 2) {
			close();
			return;
		}
		this.runningJvm = new RunningJvm(id[0], id[1]);
		final Consumer<RunningJvm> onRegister = this.onRegister;
		if (onRegister != null) {
			onRegister.accept(this.runningJvm);
		}
	}

	@Override
	public <T> void sendPacket(int packetType, T[] packet) {
		final PacketResponseHandler<T> packetResponseHandler = (PacketResponseHandler<T>) packetResponseHandlers.get(
				packetType);
		if (packetResponseHandler != null) {
			packetResponseHandler.onPacketReceived(packet);
		}
		else {
			log.warn("Received packets but no packet handler set");
		}
	}

	@Override
	public void endPacketTransfer(int packetType, int packetsSent) {
		final PacketResponseHandler<?> packetResponseHandler = packetResponseHandlers.get(packetType);
		if (packetResponseHandler != null) {
			log.debug("Received all packets for {}", packetType);
			packetResponseHandler.receivedEnd(packetsSent);
		}
		else {
			log.warn("Received packets but no packet handler set");
		}
	}

	@Override
	public void close() {
		super.close();
		packetResponseHandlers.values().forEach(PacketResponseHandler::interrupt);
		packetResponseHandlers.clear();
	}

	public boolean isRegistered() {
		return runningJvm != null;
	}

	public <T> Stream<T> getPacketStream(int packetType, Consumer<Integer> onUpdateCount) {
		final AtomicReference<Future<?>> scheduledCleanup = new AtomicReference<>();
		final PacketResponseHandler<T> packetResponseHandler = new PacketResponseHandler<>(() -> {
			log.debug("Cleaning up packet stream for {}", packetType);
			packetResponseHandlers.remove(packetType);
			final Future<?> cleanup = scheduledCleanup.get();
			if (cleanup != null) {
				log.debug("Cancelling cleanup task for {}", packetType);
				cleanup.cancel(false);
			}
		}, onUpdateCount);
		packetResponseHandlers.put(packetType, packetResponseHandler);
		final Future<?> cleanup = executorService.schedule(packetResponseHandler::interrupt, 310, TimeUnit.SECONDS);
		scheduledCleanup.set(cleanup);
		getJvmConnection().requestPackets(packetType);
		// Exports can take some time
		return packetResponseHandler.getPacketStream(300, TimeUnit.SECONDS);
	}

}
