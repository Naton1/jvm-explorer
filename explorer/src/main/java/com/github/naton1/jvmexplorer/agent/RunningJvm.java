package com.github.naton1.jvmexplorer.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Properties;

@Value
@Slf4j
public class RunningJvm {

	private final String id;
	private final String name;

	public int getJavaVersion() throws AgentException {
		try {
			String version = getSystemProperties().getProperty("java.version");
			if (version == null) {
				throw new AgentException("Target JVM does not define a java.version property");
			}
			if (version.startsWith("1.")) {
				version = version.substring(2, 3);
			}
			else {
				final int dot = version.indexOf(".");
				if (dot != -1) {
					version = version.substring(0, dot);
				}
			}
			return Integer.parseInt(version);
		}
		catch (AgentException e) {
			log.warn("Failed to get java version for remote code execution", e);
			throw e;
		}
	}

	public Properties getSystemProperties() throws AgentException {
		try {
			final VirtualMachine vm = VirtualMachine.attach(id);
			try {
				return vm.getSystemProperties();
			}
			finally {
				vm.detach();
			}
		}
		catch (AttachNotSupportedException | IOException e) {
			log.debug("Failed to load system properties", e);
			throw new AgentException(e.getMessage(), e);
		}
	}

	public void loadAgent(String agentPath, String agentArgs) throws AgentException {
		try {
			log.debug("Attempting to load agent {} with args {} into {}", agentPath, agentArgs, this);
			final VirtualMachine vm = VirtualMachine.attach(id);
			try {
				vm.loadAgent(agentPath, agentArgs);
				log.debug("Loaded agent: {}", this);
			}
			finally {
				vm.detach();
			}
		}
		catch (IOException | AttachNotSupportedException | AgentLoadException | AgentInitializationException e) {
			if (e instanceof AgentLoadException && "0".equals(e.getMessage())) {
				log.debug("Received AgentLoadException while attaching but message is '0'.", e);
				// See https://stackoverflow.com/questions/54340438
				// The implementation of attaching changed so when attaching to older JVMs (like java 8).
				// Older JVMs looked for '0' but newer JVMs are looking for 'return code: 0' when attaching.
				return;
			}
			log.warn("Failed to attach to jvm: " + this, e);
			if (JdkPatcher.patchJdkForAgent(this)) {
				log.debug("Patched target jvm, retrying load");
				loadAgent(agentPath, agentArgs);
				return;
			}
			throw new AgentException(e.getMessage(), e);
		}
	}

	@Override
	public String toString() {
		if (name == null || name.isEmpty()) {
			return id;
		}
		return name + ": " + id;
	}

	public String toIdentifier() {
		return getId() + ":" + getName();
	}

}
