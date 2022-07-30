package com.github.naton1.jvmexplorer.agent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RunningJvmTest {

	@Test
	void testIdentifier() {
		final RunningJvm runningJvm = new RunningJvm("1000", "test");

		Assertions.assertEquals("1000:test", runningJvm.toIdentifier());
	}

}