package com.github.naton1.jvmexplorer.protocol;

import lombok.Value;

@Value
public class ClassContent {

	private final String name;
	private final byte[] classContent;
	private final ClassFields classFields;

}
