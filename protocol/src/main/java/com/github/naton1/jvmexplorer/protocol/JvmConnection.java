package com.github.naton1.jvmexplorer.protocol;

// Implemented in the client
public interface JvmConnection {

	ClassContent getClassContent(ActiveClass activeClass);

	boolean setField(ClassFieldPath classFieldPath, Object newValue);

	ClassFields getFields(ClassFieldPath classFieldPath);

	byte[] getExportFile(String name);

	void requestPackets(int packetType);

	boolean redefineClass(String name, byte[] bytes);

}
