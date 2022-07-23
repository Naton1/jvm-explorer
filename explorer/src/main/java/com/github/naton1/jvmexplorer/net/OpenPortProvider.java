package com.github.naton1.jvmexplorer.net;

import java.io.IOException;
import java.net.ServerSocket;

public class OpenPortProvider {

	public int getOpenPort() throws IOException {
		try (final ServerSocket ss = new ServerSocket(0)) {
			ss.setReuseAddress(true);
			return ss.getLocalPort();
		}
	}

}
