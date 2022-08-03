package com.github.naton1.jvmexplorer.agent;

public class AgentException extends Exception {

	public AgentException(String message) {
		super(message);
	}

	public AgentException(String message, Exception source) {
		super(message, source);
	}

}
