package com.github.naton1.jvmexplorer.protocol;

import lombok.Value;

@Value
public class WrappedObject {

	private final String objectDescription;

	@Override
	public String toString() {
		return objectDescription;
	}

}
