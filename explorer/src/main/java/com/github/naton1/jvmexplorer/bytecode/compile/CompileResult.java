package com.github.naton1.jvmexplorer.bytecode.compile;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompileResult {

	private final String stdOut;
	private final boolean success;
	private final byte[] classContent;

}
