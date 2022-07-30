package com.github.naton1.jvmexplorer.integration.e2e;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.testfx.util.DebugUtils;

@Slf4j
public class ScreenshotRule implements TestWatcher {

	public void testFailed(ExtensionContext context, Throwable cause) {
		final StringBuilder debugInfo = DebugUtils.saveScreenshot().apply(new StringBuilder());
		log.info(debugInfo.toString());
	}

}
