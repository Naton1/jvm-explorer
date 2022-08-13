package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SearchClassesTest extends EndToEndTest {

	@Test
	void testSearchClasses() throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.waitForClassesToLoad();

			final long initialClasses = appHelper.streamClassTree().count();
			final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
			appHelper.searchClasses(simpleName);
			final long afterClasses = appHelper.streamClassTree().count();

			Assertions.assertTrue(afterClasses < initialClasses);
			appHelper.streamClassTree().filter(s -> s.equals(simpleName)).findFirst().orElseThrow();
		}
	}

}
