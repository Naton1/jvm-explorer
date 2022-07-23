package com.github.naton1.jvmexplorer.net;

import com.esotericsoftware.kryonet.Server;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class JvmExplorerServer extends Server {

	@Setter(AccessLevel.PACKAGE)
	@Getter
	private int port;

	public JvmExplorerServer(int writeBufferSize, int objectBufferSize) {
		super(writeBufferSize, objectBufferSize);
	}

}
