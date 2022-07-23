package com.github.naton1.jvmexplorer.protocol;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.reflect.Array;

public class Protocol {

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
		kryo.register(JvmClient.class);
		kryo.register(JvmConnection.class);
		kryo.register(PacketType.class);
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
