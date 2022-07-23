package com.github.naton1.jvmexplorer.protocol.helper;

import java.util.Arrays;

public class FieldValueHelper {

	public static String getValueAsString(Object value) {
		if (value != null && value.getClass().isArray()) {
			if (value.getClass().getComponentType().isPrimitive()) {
				final Class<?> primitiveType = value.getClass().getComponentType();
				if (primitiveType == int.class) {
					return Arrays.toString((int[]) value);
				}
				else if (primitiveType == char.class) {
					return Arrays.toString((char[]) value);
				}
				else if (primitiveType == byte.class) {
					return Arrays.toString((byte[]) value);
				}
				else if (primitiveType == boolean.class) {
					return Arrays.toString((boolean[]) value);
				}
				else if (primitiveType == long.class) {
					return Arrays.toString((long[]) value);
				}
				else if (primitiveType == short.class) {
					return Arrays.toString((short[]) value);
				}
				else if (primitiveType == double.class) {
					return Arrays.toString((double[]) value);
				}
				else if (primitiveType == float.class) {
					return Arrays.toString((float[]) value);
				}
				// We've covered all primitives. This isn't possible.
				throw new IllegalStateException();
			}
			return Arrays.deepToString((Object[]) value);
		}
		return String.valueOf(value);
	}

}
