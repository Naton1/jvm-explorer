package com.github.naton1.jvmexplorer.protocol;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.reflect.Array;

public class Protocol {

	// Kryonet requires specifying the max object size that we'll send up front.
	// This is unfortunate since we have no idea. It could definitely fail to send a class if it's extremely large.
	// A few solutions:
	// 1) Change everything to byte arrays and send them in chunks, then reprocess here
	// 2) Completely remove kryonet and roll a custom client/server
	// 3) Replace with java's RMI. Hesitant to do it because it relies on RMI being in the target JVM.
	public static final int WRITE_BUFFER_SIZE = 1024 * 1024;
	public static final int OBJECT_BUFFER_SIZE = 1024 * 1024;

	public static final int RMI_JVM_CLIENT = 1;
	public static final int RMI_JVM_CONNECTION = 2;

	private static final Class<?>[] DEFAULTS = { int.class,
	                                             boolean.class,
	                                             short.class,
	                                             long.class,
	                                             byte.class,
	                                             double.class,
	                                             float.class,
	                                             char.class,
	                                             Integer.class,
	                                             Boolean.class,
	                                             Short.class,
	                                             Long.class,
	                                             Byte.class,
	                                             Double.class,
	                                             Float.class,
	                                             Character.class,
	                                             String.class };

	public static void register(Kryo kryo) {
		// Need to bypass constructors since everything is a value object
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());

		// Setup core classes
		kryo.register(LoadedClass.class);
		kryo.register(LoadedClass[].class);
		kryo.register(ClassContent.class);
		kryo.register(ClassField.class);
		kryo.register(ClassField[].class);
		kryo.register(ClassFieldKey.class);
		kryo.register(ClassFieldKey[].class);
		kryo.register(ClassFieldPath.class);
		kryo.register(ClassFields.class);
		kryo.register(ClassLoaderDescriptor.class);
		kryo.register(ExecutionResult.class);
		kryo.register(JvmClient.class);
		kryo.register(JvmConnection.class);
		kryo.register(PacketType.class);
		kryo.register(PatchResult.class);
		kryo.register(WrappedObject.class);

		// Setup all primitives for field reading/writing
		for (Class<?> clazz : DEFAULTS) {
			kryo.register(clazz);
			final Class<?> arrayClass = Array.newInstance(clazz, 0).getClass();
			kryo.register(arrayClass);
		}

		// Setup for RMI
		ObjectSpace.registerClasses(kryo);
	}

}
