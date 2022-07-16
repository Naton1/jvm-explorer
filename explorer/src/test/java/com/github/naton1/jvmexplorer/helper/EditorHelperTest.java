package com.github.naton1.jvmexplorer.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EditorHelperTest {

	@Test
	void testGetStringAsString() {
		final EditorHelper editorHelper = new EditorHelper();
		final String testString = "test";

		final String result = editorHelper.getObjectString("java.lang.String", testString);
		// One downside of using json is there is extra quotes. May want to change this in the future.
		Assertions.assertEquals("\"" + testString + "\"", result);
	}

	@Test
	void getGetIntAsString() {
		final EditorHelper editorHelper = new EditorHelper();

		final String result = editorHelper.getObjectString("int", 5);
		Assertions.assertEquals("5", result);
	}

	@Test
	void getGetBooleanAsString() {
		final EditorHelper editorHelper = new EditorHelper();

		final String result = editorHelper.getObjectString("boolean", true);
		Assertions.assertEquals("true", result);
	}

	@Test
	void getUnknownTypeAsString() {
		final EditorHelper editorHelper = new EditorHelper();

		final String result = editorHelper.getObjectString("unknown-type", true);
		Assertions.assertNull(result);
	}

	@Test
	void parseString() {
		final EditorHelper editorHelper = new EditorHelper();
		final String newValue = "test";

		final Object result = editorHelper.edit("java.lang.String", newValue);
		Assertions.assertEquals(newValue, result);
	}

	@Test
	void parseInt() {
		final EditorHelper editorHelper = new EditorHelper();

		final Object result = editorHelper.edit("int", "5");
		Assertions.assertEquals(5, result);
	}

	@Test
	void parseBoolean() {
		final EditorHelper editorHelper = new EditorHelper();

		final Object result = editorHelper.edit("boolean", "true");
		Assertions.assertEquals(true, result);
	}

}