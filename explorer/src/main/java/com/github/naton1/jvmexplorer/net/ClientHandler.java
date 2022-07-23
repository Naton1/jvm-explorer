package com.github.naton1.jvmexplorer.net;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.JvmConnection;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.protocol.PacketType;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Builder
@Slf4j
public class ClientHandler extends Listener {

	private final Set<JvmClientImpl> clients = ConcurrentHashMap.newKeySet();

	private final BiConsumer<RunningJvm, Connection> onConnect;
	private final Consumer<RunningJvm> onDisconnect;

	public ClassContent getClassContent(RunningJvm runningJvm, LoadedClass loadedClass) {
		return getJvmConnection(runningJvm).map(j -> j.getClassContent(loadedClass)).orElse(null);
	}

	private Optional<JvmConnection> getJvmConnection(RunningJvm runningJvm) {
		return getServerTracker(runningJvm).map(JvmClientImpl::getJvmConnection);
	}

	private Optional<JvmClientImpl> getServerTracker(RunningJvm runningJvm) {
		return clients.stream()
		              .filter(JvmClientImpl::isRegistered)
		              .filter(s -> s.getRunningJvm().equals(runningJvm))
		              .findFirst();
	}

	public boolean setField(RunningJvm runningJvm, ClassFieldPath classFieldPath, Object newValue) {
		return getJvmConnection(runningJvm).map(j -> j.setField(classFieldPath, newValue)).orElse(false);
	}

	public ClassFields getFields(RunningJvm runningJvm, ClassFieldPath classFieldPath) {
		return getJvmConnection(runningJvm).map(j -> j.getFields(classFieldPath)).orElse(null);
	}

	public byte[] getClassBytes(RunningJvm runningJvm, LoadedClass loadedClass) {
		return getJvmConnection(runningJvm).map(j -> j.getClassBytes(loadedClass)).orElse(null);
	}

	public List<LoadedClass> getLoadedClasses(RunningJvm runningJvm, Consumer<Integer> onUpdateCount) {
		return getServerTracker(runningJvm).map(serverTracker -> serverTracker.<LoadedClass>getPacketStream(PacketType.LOADED_CLASSES,
		                                                                                                    onUpdateCount))
		                                   .map(str -> str.collect(Collectors.toList()))
		                                   .orElse(null);
	}

	public void close(RunningJvm runningJvm) {
		getServerTracker(runningJvm).ifPresent(Connection::close);
	}

	public boolean replaceClass(RunningJvm runningJvm, LoadedClass loadedClass, byte[] bytes) {
		return getJvmConnection(runningJvm).map(jvmConnection -> jvmConnection.redefineClass(loadedClass, bytes))
		                                   .orElse(false);
	}

	@Override
	public void connected(Connection connection) {
		final JvmClientImpl serverTrackerImpl = (JvmClientImpl) connection;
		clients.add(serverTrackerImpl);
		serverTrackerImpl.setOnRegister(jvm -> this.onConnect.accept(jvm, connection));
	}

	@Override
	public void disconnected(Connection connection) {
		final JvmClientImpl serverTrackerImpl = (JvmClientImpl) connection;
		clients.remove(serverTrackerImpl);
		if (serverTrackerImpl.isRegistered()) {
			onDisconnect.accept(serverTrackerImpl.getRunningJvm());
		}
	}

}
