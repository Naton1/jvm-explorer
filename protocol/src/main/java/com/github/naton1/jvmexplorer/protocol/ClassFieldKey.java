package com.github.naton1.jvmexplorer.protocol;

import lombok.Value;

import java.lang.reflect.Modifier;

@Value
public class ClassFieldKey {

	private final String className;
	private final String fieldName;
	private final String typeName;
	private final int modifiers;

	@Override
	public String toString() {
		return Modifier.toString(modifiers) + " " + typeName + " " + getSimpleName() + "." + fieldName;
	}

	public String getSimpleName() {
		if (className.contains(".")) {
			final int startIndex = className.lastIndexOf('.') + 1;
			if (startIndex != className.length()) {
				return className.substring(startIndex);
			}
		}
		return className;
	}

}
