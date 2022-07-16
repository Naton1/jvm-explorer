package com.github.naton1.jvmexplorer.protocol;

import lombok.Value;
import lombok.With;

import java.util.Arrays;

@Value
public class ClassField {

	private final ClassFieldKey classFieldKey;

	@With
	private final Object value;

	@Override
	public String toString() {
		return classFieldKey + " = " + getValueAsString().replace("\n", "");
	}

	private String getValueAsString() {
		if (value != null && value.getClass().isArray()) {
			if (value.getClass().getComponentType().isPrimitive()) {
				try {
					// Dynamically resolve the appropriate method rather than hardcode every single case
					return (String) Arrays.class.getDeclaredMethod("toString", value.getClass()).invoke(null, value);
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			return Arrays.deepToString((Object[]) value);
		}
		return String.valueOf(value);
	}

}
