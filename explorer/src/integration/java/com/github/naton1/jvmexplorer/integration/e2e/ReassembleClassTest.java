package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReassembleClassTest extends EndToEndTest {

	@Test
	void testReassembleClass() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.openTab(1);
			appHelper.waitUntilDisassemble();
			Assertions.assertFalse(appHelper.isTabChangesPending(1));
			appHelper.patchBytecode(s -> s.replaceFirst("version \\d+:\\d", "version 48:0"));
			Assertions.assertTrue(appHelper.isTabChangesPending(1));
			appHelper.saveBytecodeChanges();
			appHelper.waitUntilChangesNotPending(1);
		}
	}

	@Test
	void testResetChanges() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.openTab(1);
			appHelper.waitUntilDisassemble();
			Assertions.assertFalse(appHelper.isTabChangesPending(1));
			appHelper.patchBytecode(s -> s.replaceFirst("version \\d+:\\d", "version 48:0"));
			Assertions.assertTrue(appHelper.isTabChangesPending(1));
			appHelper.resetBytecodeChanges();
			appHelper.waitUntilChangesNotPending(1);
		}
	}

}


