package com.github.naton1.jvmexplorer.protocol;

import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import lombok.Value;

@Value
public class ActiveClass implements Comparable<ActiveClass> {

	private final String name;

	public String getSimpleName() {
		return ClassNameHelper.getSimpleName(name);
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
