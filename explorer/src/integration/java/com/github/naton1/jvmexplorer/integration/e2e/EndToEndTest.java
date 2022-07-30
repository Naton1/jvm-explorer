package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.JvmExplorer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

@ExtendWith(ApplicationExtension.class)
abstract class EndToEndTest {

	@BeforeEach
	void setup() throws Exception {
		WaitForAsyncUtils.autoCheckException = true;
		WaitForAsyncUtils.checkAllExceptions = true;
		FxToolkit.setupApplication(JvmExplorer.class);
	}

	@AfterEach
	void teardown() throws Exception {
		// We don't care if there's an unhandled exception while cleaning up.
		// (ex. server closed)
		WaitForAsyncUtils.autoCheckException = false;
		WaitForAsyncUtils.checkAllExceptions = false;
		FxToolkit.cleanupStages();
	}

}
