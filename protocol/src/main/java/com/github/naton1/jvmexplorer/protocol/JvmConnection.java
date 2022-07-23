package com.github.naton1.jvmexplorer.protocol;

// Implemented in the client
public interface JvmConnection {

	ClassContent getClassContent(LoadedClass loadedClass);

	boolean setField(ClassFieldPath classFieldPath, Object newValue);

	ClassFields getFields(ClassFieldPath classFieldPath);

	byte[] getClassBytes(LoadedClass loadedClass);

	void requestPackets(PacketType packetType);

	boolean redefineClass(LoadedClass loadedClass, byte[] bytes);

}
