package com.github.naton1.jvmexplorer.protocol;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(of = "id")
@Builder
public class ClassLoaderDescriptor {

	private final String id;
	private final String description;

	private final ClassLoaderDescriptor parent;

	@Override
	public String toString() {
		return description;
	}

}
