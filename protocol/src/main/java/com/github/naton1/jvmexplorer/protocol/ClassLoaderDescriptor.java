package com.github.naton1.jvmexplorer.protocol;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

@Value
@EqualsAndHashCode(of = "id")
@ToString(of = "description")
@Builder
public class ClassLoaderDescriptor {

	private final String id;
	private final String description;

}
