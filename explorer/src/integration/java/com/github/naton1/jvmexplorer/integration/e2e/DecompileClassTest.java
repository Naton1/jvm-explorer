package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class DecompileClassTest extends EndToEndTest {

	@Test
	void testProcessAppears_classesLoad_classDecompiles() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();
			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			appHelper.selectClass(simpleName);

			// Verify decompile
			appHelper.waitUntilJavaCodeContains("class " + simpleName);

			// Verify disassemble
			appHelper.openTab(1);
			appHelper.waitUntilByteCodeContains("class " + simpleName);

			// Verify fields
			appHelper.openTab(2);
			Assertions.assertTrue(appHelper.getFieldTree().getRoot().getChildren().size() > 0);
		}
	}

}
