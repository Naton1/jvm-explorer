package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import org.junit.jupiter.api.Test;

class ReloadClassesTest extends EndToEndTest {

	@Test
	void testReloadClasses() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();
			appHelper.selectClassAction("Refresh Classes");
			appHelper.waitForClassesToBeEmpty();
			appHelper.waitForClassesToLoad();
		}
	}

}
