package com.github.naton1.jvmexplorer.protocol;

import lombok.Value;

@Value
public class ClassFieldPath {

	private final ClassFieldKey[] classFieldKeys;
	private final ClassLoaderDescriptor classLoaderDescriptor;

}
