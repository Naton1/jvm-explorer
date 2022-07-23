package com.github.naton1.jvmexplorer.settings;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class JvmExplorerSettings {

	private final boolean showClassLoader;

}
