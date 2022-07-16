package com.github.naton1.jvmexplorer.protocol;

import lombok.Value;

@Value
public class ActiveClass implements Comparable<ActiveClass> {

	private final String name;

	public String getSimpleName() {
		final int lastPackagePart = name.lastIndexOf('.');
		if (lastPackagePart != -1) {
			return name.substring(lastPackagePart + 1);
		}
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(ActiveClass o) {
		return name.compareTo(o.name);
	}

}
