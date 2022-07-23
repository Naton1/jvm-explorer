package com.github.naton1.jvmexplorer.protocol;

import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
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
		return ClassNameHelper.getSimpleName(className);
	}

}
