package com.github.naton1.jvmexplorer.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

@Value
@Slf4j
public class RunningJvm {

	private static final AgentPreparer agentPreparer = new AgentPreparer();
	private static final String PATH = "agents/agent.jar";

	private final String id;
	private final String name;

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

	public void loadAgent() throws AgentException {
		try {
			log.debug("Attempting to load agent: {}", this);
			final String localPath = agentPreparer.loadAgentOnFileSystem(PATH);
			final VirtualMachine vm = VirtualMachine.attach(id);
			vm.loadAgent(localPath, id + ":" + name);
			vm.detach();
			log.debug("Loaded agent: {}", this);
		}
		catch (IOException | UncheckedIOException | AttachNotSupportedException | AgentLoadException | AgentInitializationException e) {
			log.warn("Failed to attach to jvm: " + this, e);
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

}
