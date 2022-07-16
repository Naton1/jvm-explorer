package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.ActiveClass;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import com.github.naton1.jvmexplorer.protocol.JvmConnection;
import com.github.naton1.jvmexplorer.protocol.Protocol;
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

	@Override
	public ClassContent getClassContent(ActiveClass activeClass) {
		final Class<?> klass = instrumentationHelper.getClassByName(activeClass.getName());
		if (klass == null) {
			Log.warn("Failed to find class: " + activeClass);
			return null;
		}
		final byte[] classContent = instrumentationHelper.getClassBytes(klass);
		final ClassFields classFields = instrumentationHelper.getClassFields(klass, null);
		return new ClassContent(activeClass.getName(), classContent, classFields);
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
	public void requestPackets(int packetType) {
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

	private void processActiveClassPackets(int packetType) {
		final List<Class<?>> applicationClasses = instrumentationHelper.getApplicationClasses();
		final List<ActiveClass> classes = new ArrayList<>();
		for (Class<?> c : applicationClasses) {
			final String className = c.getName();
			final ActiveClass activeClass = new ActiveClass(className);
			classes.add(activeClass);
		}
		final int packetSize = 100;
		final int packetCount = (int) Math.ceil(classes.size() / (double) packetSize);
		final Queue<ActiveClass[]> activeClassPackets = new ConcurrentLinkedQueue<>();
		final IdlePacketSender<ActiveClass> idlePacketSender = new IdlePacketSender<>(activeClassPackets,
		                                                                              packetType,
		                                                                              jvmClient,
		                                                                              false);
		client.addListener(idlePacketSender);
		try {
			for (int i = 0; i < packetCount; i++) {
				final ActiveClass[] activeClasses = classes.subList(i * packetSize,
				                                                    Math.min((i + 1) * packetSize, classes.size()))
				                                           .toArray(new ActiveClass[0]);
				activeClassPackets.add(activeClasses);
			}
		}
		finally {
			idlePacketSender.end();
		}
	}

	@RequiredArgsConstructor
	private class PacketProcessor implements Runnable {
		private final int packetType;

		@Override
		public void run() {
			Log.debug("Received packet request for " + packetType);
			try {
				// Note: could probably generalize the logic for packets in the future, if needed
				switch (packetType) {
				case Protocol.PACKET_TYPE_ACTIVE_CLASSES:
					processActiveClassPackets(packetType);
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