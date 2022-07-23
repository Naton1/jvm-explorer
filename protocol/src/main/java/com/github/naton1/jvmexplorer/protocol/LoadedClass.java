package com.github.naton1.jvmexplorer.protocol;

import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import lombok.Value;

@Value
public class LoadedClass implements Comparable<LoadedClass> {

	private final String name;
	private final ClassLoaderDescriptor classLoaderDescriptor;

	public String getSimpleName() {
		return ClassNameHelper.getSimpleName(name);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(LoadedClass o) {
		return name.compareTo(o.name);
	}

}
