package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class ImportExportTest extends EndToEndTest {

	@Test
	void testGlobalExportThenPatch() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();

			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			appHelper.searchClasses(simpleName);

			// Make sure it works with both classloader settings

			appHelper.disableClassLoaders();
			doJarExportThenImport("Export Classes", "Replace Classes");

			appHelper.enableClassLoaders();
			doJarExportThenImport("Export Classes", "Replace Classes");
		}
	}

	@Test
	void testExportClassThenPatch() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();
			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			appHelper.selectClass(simpleName);

			final File tempClass = appHelper.createTempClass();
			appHelper.setTestFile(tempClass);

			appHelper.selectClassAction("Export Class");

			fxRobot.waitUntil(() -> tempClass.length() > 0, 5000);

			appHelper.setTestFile(tempClass);
			appHelper.selectClassAction("Replace Class");

			appHelper.waitForAlert("Replaced Class", "Successfully replaced class");
		}
	}

	@Test
	void testExportPackageThenPatch() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();

			final String packageName = ClassNameHelper.getPackageName(testJvm.getMainClassName());
			final String simplePackageName = ClassNameHelper.getSimpleName(packageName);
			appHelper.selectClass(simplePackageName);

			doJarExportThenImport("Export Package", "Replace Package");
		}
	}

	@Test
	void testExportClassLoaderThenPatch() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();

			appHelper.enableClassLoaders();

			appHelper.selectClass("AppClassLoader");

			doJarExportThenImport("Export Class Loader", "Replace Class Loader");
		}
	}

	private void doJarExportThenImport(String exportAction, String importAction) {
		final File tempJar = appHelper.createTempJar();
		appHelper.setTestFile(tempJar);

		appHelper.selectClassAction(exportAction);

		fxRobot.waitUntil(() -> tempJar.length() > 0, 5000);
		fxRobot.waitForStageExists("Export Finished");
		final long exportedClasses = appHelper.streamClasses(tempJar).count();
		Assertions.assertTrue(exportedClasses > 0);

		log.info("Exported {} classes", exportedClasses);

		appHelper.setTestFile(tempJar);
		appHelper.selectClassAction(importAction);

		fxRobot.waitForStageExists("Patching Finished");
		appHelper.waitForAlert("Patching Finished", "Patch succeeded");
		log.info("Patched {} classes", exportedClasses);
	}

}
