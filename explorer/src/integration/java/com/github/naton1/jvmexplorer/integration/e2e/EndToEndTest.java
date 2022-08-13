package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.JvmExplorer;
import com.github.naton1.jvmexplorer.integration.helper.AppHelper;
import com.github.naton1.jvmexplorer.integration.helper.FxRobotPlus;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.DebugUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(ApplicationExtension.class)
@Slf4j
abstract class EndToEndTest {

	private static final AtomicInteger testNumber = new AtomicInteger();
	private static final Path SCREENSHOT_DIR = Paths.get("integration-test-screenshots");

	protected FxRobotPlus fxRobot;
	protected AppHelper appHelper;

	static {
		SCREENSHOT_DIR.toFile().mkdirs();
		try {
			for (Path path : Files.newDirectoryStream(SCREENSHOT_DIR, f -> f.toFile().isFile())) {
				Files.delete(path);
			}
		}
		catch (IOException e) {
			log.warn("Failed to clean up screenshot dir", e);
		}
	}

	@BeforeEach
	void setup(FxRobot fxRobot) throws Exception {
		log.debug("Setting up test");
		// Closing the stage kills the executor service, and highlighting throws an unhandled exception causing the
		// test to fail
		WaitForAsyncUtils.autoCheckException = false;
		FxToolkit.setupApplication(JvmExplorer.class);
		this.fxRobot = new FxRobotPlus(fxRobot);
		this.appHelper = new AppHelper(this.fxRobot);
	}

	@AfterEach
	void teardown() throws Exception {
		final StringBuilder debugInfo = DebugUtils.saveScreenshot(this::getScreenshotPath, "   ")
		                                          .apply(new StringBuilder());
		log.info(debugInfo.toString());
		log.debug("Tearing down test");
		FxToolkit.cleanupStages();
	}

	private Path getScreenshotPath() {
		return SCREENSHOT_DIR.resolve(getClass().getName() + " - " + testNumber.incrementAndGet() + ".png");
	}

}
