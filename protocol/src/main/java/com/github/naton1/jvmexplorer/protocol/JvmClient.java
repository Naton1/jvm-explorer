package com.github.naton1.jvmexplorer.protocol;

// Implemented in the server
public interface JvmClient {

	void register(String identifier);

	<T> void sendPacket(PacketType packetType, T[] packets);

	void endPacketTransfer(PacketType packetType, int packetsSent);

}
