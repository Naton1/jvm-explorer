package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import org.junit.jupiter.api.Test;

class ShowClassLoaderTest extends EndToEndTest {

	@Test
	void testShowClassLoader() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();
			appHelper.disableClassLoaders();
			appHelper.selectClassAction("Show Class Loaders");
			appHelper.waitForClassesToLoad();
			fxRobot.waitUntil(appHelper::isClassLoadersVisible, 5000);
			appHelper.selectClassAction("Show Class Loaders");
			appHelper.waitForClassesToLoad();
			fxRobot.waitUntil(() -> !appHelper.isClassLoadersVisible(), 5000);
		}
	}

}
