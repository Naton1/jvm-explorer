package com.github.naton1.jvmexplorer.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RemoteCodeTemplateHelperTest {

	@Test
	void testPackage() {
		final RemoteCodeTemplateHelper remoteCodeTemplateHelper = new RemoteCodeTemplateHelper();
		final String template = remoteCodeTemplateHelper.load("mypackage", "ClassName");

		Assertions.assertTrue(template.contains("package mypackage;"));
	}

	@Test
	void testEmptyPackage() {
		final RemoteCodeTemplateHelper remoteCodeTemplateHelper = new RemoteCodeTemplateHelper();
		final String template = remoteCodeTemplateHelper.load(null, "ClassName");

		Assertions.assertFalse(template.contains("package"));
	}

	@Test
	void testClassName() {
		final RemoteCodeTemplateHelper remoteCodeTemplateHelper = new RemoteCodeTemplateHelper();
		final String template = remoteCodeTemplateHelper.load("", "ClassName");

		Assertions.assertTrue(template.contains("class ClassName"));
	}

}