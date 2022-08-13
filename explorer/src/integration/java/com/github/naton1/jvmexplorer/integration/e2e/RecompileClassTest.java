package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RecompileClassTest extends EndToEndTest {

	@Test
	void testRecompileClass() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.waitUntilDecompile();
			Assertions.assertFalse(appHelper.isTabChangesPending(0));
			appHelper.patchCode(s -> s.replace("return arg;", "return arg + arg;"));
			Assertions.assertTrue(appHelper.isTabChangesPending(0));
			appHelper.saveCodeChanges();
			appHelper.waitUntilChangesNotPending(0);
		}
	}

	@Test
	void testResetChanges() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.waitUntilDecompile();
			Assertions.assertFalse(appHelper.isTabChangesPending(0));
			appHelper.patchCode(s -> s.replace("return arg;", "return arg + arg;"));
			Assertions.assertTrue(appHelper.isTabChangesPending(0));
			appHelper.resetCodeChanges();
			appHelper.waitUntilChangesNotPending(0);
		}
	}

}


