package com.github.naton1.jvmexplorer.protocol;

import com.github.naton1.jvmexplorer.protocol.helper.FieldValueHelper;
import lombok.Value;
import lombok.With;

@Value
public class ClassField {

	private final ClassFieldKey classFieldKey;

	@With
	private final Object value;

	@Override
	public String toString() {
		return classFieldKey + " = " + FieldValueHelper.getValueAsString(value).replace("\n", "");
	}

}
