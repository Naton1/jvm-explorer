package com.github.naton1.jvmexplorer.protocol;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PatchResult {

	private final boolean success;
	private final String message;

}
