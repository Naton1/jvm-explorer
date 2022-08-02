package com.github.naton1.jvmexplorer.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CodeTemplateHelperTest {

	@Test
	void testPackage() {
		final CodeTemplateHelper codeTemplateHelper = new CodeTemplateHelper();
		final String template = codeTemplateHelper.loadRemoteCallable("mypackage", "ClassName");

		Assertions.assertTrue(template.contains("package mypackage;"));
	}

	@Test
	void testEmptyPackage() {
		final CodeTemplateHelper codeTemplateHelper = new CodeTemplateHelper();
		final String template = codeTemplateHelper.loadRemoteCallable(null, "ClassName");

		Assertions.assertFalse(template.contains("package"));
	}

	@Test
	void testClassName() {
		final CodeTemplateHelper codeTemplateHelper = new CodeTemplateHelper();
		final String template = codeTemplateHelper.loadRemoteCallable("", "ClassName");

		Assertions.assertTrue(template.contains("class ClassName"));
	}

}