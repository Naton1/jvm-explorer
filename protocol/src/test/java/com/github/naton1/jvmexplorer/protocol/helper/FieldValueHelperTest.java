package com.github.naton1.jvmexplorer.protocol.helper;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class FieldValueHelperTest {

	@Test
	public void testValueAsStringWithNull() {
		final String valueAsString = FieldValueHelper.getValueAsString(null);
		Assert.assertEquals("null", valueAsString);
	}

	@Test
	public void testValueAsStringWithString() {
		final String string = "some-string";
		final String valueAsString = FieldValueHelper.getValueAsString(string);
		Assert.assertEquals(string, valueAsString);
	}

	@Test
	public void testValueAsStringWithBoolean() {
		final String valueAsString = FieldValueHelper.getValueAsString(true);
		Assert.assertEquals("true", valueAsString);
	}

	@Test
	public void testValueAsStringWithCharArray() {
		final String valueAsString = FieldValueHelper.getValueAsString(new char[] { 'a', 'b', 'c' });
		Assert.assertEquals("[a, b, c]", valueAsString);
	}

	@Test
	public void testValueAsStringWithDoubleArray() {
		final String valueAsString = FieldValueHelper.getValueAsString(new double[] { 0.5, 0.3 });
		Assert.assertEquals("[0.5, 0.3]", valueAsString);
	}

	@Test
	public void testValueAsStringWith2DArray() {
		final String valueAsString = FieldValueHelper.getValueAsString(new int[][] { { 3, 4 }, { 5, 6 } });
		Assert.assertEquals("[[3, 4], [5, 6]]", valueAsString);
	}

	@Test
	public void testValueAsStringWithObject() {
		final List<String> list = Arrays.asList("a", "string", "list");
		final String valueAsString = FieldValueHelper.getValueAsString(list);
		Assert.assertEquals(list.toString(), valueAsString);
	}

}