package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.ClassContent;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldPath;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.ExecutionResult;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import com.github.naton1.jvmexplorer.protocol.JvmConnection;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.protocol.PacketType;
import com.github.naton1.jvmexplorer.protocol.PatchResult;
import lombok.RequiredArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
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
		try {
			Log.debug("Getting class content for: " + loadedClass);
			final ClassLoader classLoader = classLoaderStore.lookup(loadedClass.getClassLoaderDescriptor());
			final Class<?> klass = instrumentationHelper.getClassByName(loadedClass.getName(), classLoader);
			if (klass == null) {
				Log.warn("Failed to find class: " + loadedClass);
				return null;
			}
			Log.debug("Found class: " + klass);
			final byte[] classContent = instrumentationHelper.getClassBytes(klass);
			Log.debug("Found class bytes for: " + klass);
			final ClassFields classFields = instrumentationHelper.getClassFields(klass, null);
			Log.debug("Found class fields for: " + klass);
			return new ClassContent(loadedClass, classContent, classFields);
		}
		catch (Throwable t) {
			Log.error("Caught", t);
			throw t;
		}
	}

	@Override
	public boolean setField(ClassFieldPath classFieldPath, Object newValue) {
		final ClassLoader classLoader = classLoaderStore.lookup(classFieldPath.getClassLoaderDescriptor());
		return instrumentationHelper.setObject(classLoader, classFieldPath.getClassFieldKeys(), newValue);
	}

	@Override
	public ClassFields getFields(ClassFieldPath classFieldPath) {
		final ClassLoader classLoader = classLoaderStore.lookup(classFieldPath.getClassLoaderDescriptor());
		final ClassFields classFields = instrumentationHelper.getClassFields(classLoader,
		                                                                     classFieldPath.getClassFieldKeys());
		if (classFields == null) {
			Log.warn("Failed to find fields: " + classFieldPath);
			return new ClassFields(new ClassField[0]);
		}
		return classFields;
	}

	@Override
	public byte[] getClassBytes(LoadedClass loadedClass) {
		final ClassLoader classLoader = classLoaderStore.lookup(loadedClass.getClassLoaderDescriptor());
		final Class<?> klass = instrumentationHelper.getClassByName(loadedClass.getName(), classLoader);
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
	public PatchResult redefineClass(LoadedClass loadedClass, byte[] bytes) {
		final ClassLoader classLoader = classLoaderStore.lookup(loadedClass.getClassLoaderDescriptor());
		final Class<?> klass = instrumentationHelper.getClassByName(loadedClass.getName(), classLoader);
		if (klass == null) {
			return PatchResult.builder().success(false).message("Failed to find class: " + loadedClass).build();
		}
		return instrumentationHelper.redefineClass(klass, bytes);
	}

	@Override
	public ExecutionResult executeCallable(String className, byte[] classFile,
	                                       ClassLoaderDescriptor classLoaderDescriptor) {
		final ClassLoader parentClassLoader = classLoaderStore.lookup(classLoaderDescriptor);
		final ClassLoader classLoader = new SingleClassLoader(parentClassLoader, className, classFile);
		try {
			final Class<?> klass = classLoader.loadClass(className);
			final Callable<Object> callable = (Callable<Object>) klass.getConstructor().newInstance();
			final Object result = callable.call();
			return ExecutionResult.builder().success(true).message(String.valueOf(result)).build();
		}
		catch (Throwable e) {
			Log.warn("Failed to execute class", e);
			final StringWriter stringWriter = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
			return ExecutionResult.builder().success(false).message(stringWriter.toString()).build();
		}
	}

	private void processLoadedClassPackets(PacketType packetType) {
		final List<Class<?>> applicationClasses = instrumentationHelper.getApplicationClasses();
		final List<LoadedClass> classes = new ArrayList<>();
		for (Class<?> c : applicationClasses) {
			final String className = c.getName();
			final ClassLoaderDescriptor classLoaderDescriptor =
					c.getClassLoader() != null ? classLoaderStore.store(c.getClassLoader()) : null;
			final LoadedClass loadedClass = new LoadedClass(className,
			                                                classLoaderDescriptor,
			                                                LoadedClass.MetaType.getFor(c));
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