package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.JvmExplorer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(ScreenshotRule.class)
@Slf4j
abstract class EndToEndTest {

	@BeforeEach
	void setup() throws Exception {
		log.debug("Setting up test");
		// Closing the stage kills the executor service, and highlighting throws an unhandled exception causing the
		// test to fail
		WaitForAsyncUtils.autoCheckException = false;
		FxToolkit.setupApplication(JvmExplorer.class);
	}

	@AfterEach
	void teardown() throws Exception {
		log.debug("Tearing down test");
		FxToolkit.cleanupStages();
	}

}
