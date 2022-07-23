package com.github.naton1.jvmexplorer.protocol.helper;

public class ClassNameHelper {

	public static String getSimpleName(String name) {
		final int lastIndex = name.lastIndexOf('.');
		if (lastIndex == name.length()) {
			// Somehow the '.' is the last name. Should never happen but let's be safe.
			return "";
		}
		return name.substring(name.lastIndexOf('.') + 1);
	}

}
