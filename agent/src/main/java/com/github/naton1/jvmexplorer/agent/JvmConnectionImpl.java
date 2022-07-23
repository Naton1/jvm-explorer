package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import com.github.naton1.jvmexplorer.protocol.JvmConnection;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.protocol.PacketType;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class JvmConnectionImpl implements JvmConnection {

	private final JvmClient jvmClient;
	private final InstrumentationHelper instrumentationHelper;
	private final Client client;
	private final ExecutorService executorService;
	private final ClassLoaderStore classLoaderStore;

	@Override
	public ClassContent getClassContent(LoadedClass loadedClass) {
		final Class<?> klass = instrumentationHelper.getClassByName(loadedClass.getName());
		if (klass == null) {
			Log.warn("Failed to find class: " + loadedClass);
			return null;
		}
		final byte[] classContent = instrumentationHelper.getClassBytes(klass);
		final ClassFields classFields = instrumentationHelper.getClassFields(klass, null);
		return new ClassContent(loadedClass.getName(), classContent, classFields);
	}

	@Override
	public boolean setField(ClassFieldPath classFieldPath, Object newValue) {
		return instrumentationHelper.setObject(classFieldPath, newValue);
	}

	@Override
	public ClassFields getFields(ClassFieldPath classFieldPath) {
		final ClassFields classFields = instrumentationHelper.getClassFields(classFieldPath);
		if (classFields == null) {
			Log.warn("Failed to find fields: " + classFieldPath);
			return new ClassFields(new ClassField[0]);
		}
		return classFields;
	}

	@Override
	public byte[] getExportFile(String name) {
		final Class<?> klass = instrumentationHelper.getClassByName(name);
		if (klass == null) {
			return null;
		}
		return instrumentationHelper.getClassBytes(klass);
	}

	@Override
	public void requestPackets(PacketType packetType) {
		executorService.submit(new PacketProcessor(packetType));
	}

	@Override
	public boolean redefineClass(String name, byte[] bytes) {
		final Class<?> klass = instrumentationHelper.getClassByName(name);
		if (klass == null) {
			return false;
		}
		return instrumentationHelper.redefineClass(klass, bytes);
	}

	private void processLoadedClassPackets(PacketType packetType) {
		final List<Class<?>> applicationClasses = instrumentationHelper.getApplicationClasses();
		final List<LoadedClass> classes = new ArrayList<>();
		for (Class<?> c : applicationClasses) {
			final String className = c.getName();
			final ClassLoaderDescriptor classLoaderDescriptor =
					c.getClassLoader() != null ? classLoaderStore.store(c.getClassLoader()) : null;
			final LoadedClass loadedClass = new LoadedClass(className, classLoaderDescriptor);
			classes.add(loadedClass);
		}
		final int packetSize = 100;
		final int packetCount = (int) Math.ceil(classes.size() / (double) packetSize);
		final Queue<LoadedClass[]> loadedClassPackets = new ConcurrentLinkedQueue<>();
		final IdlePacketSender<LoadedClass> idlePacketSender = new IdlePacketSender<>(loadedClassPackets,
		                                                                              packetType,
		                                                                              jvmClient,
		                                                                              false);
		client.addListener(idlePacketSender);
		try {
			for (int i = 0; i < packetCount; i++) {
				final LoadedClass[] loadedClasses = classes.subList(i * packetSize,
				                                                    Math.min((i + 1) * packetSize, classes.size()))
				                                           .toArray(new LoadedClass[0]);
				loadedClassPackets.add(loadedClasses);
			}
		}
		finally {
			idlePacketSender.end();
		}
	}

	@RequiredArgsConstructor
	private class PacketProcessor implements Runnable {
		private final PacketType packetType;

		@Override
		public void run() {
			Log.debug("Received packet request for " + packetType);
			try {
				// Note: could probably generalize the logic for packets in the future, if needed
				switch (packetType) {
				case LOADED_CLASSES:
					processLoadedClassPackets(packetType);
					break;
				default:
					Log.warn("Unknown packet type: " + packetType);
					break;
				}
			}
			catch (Exception e) {
				Log.warn("Caught exception while initializing packet transfer", e);
				jvmClient.endPacketTransfer(packetType, 0);
			}
		}
	}

}