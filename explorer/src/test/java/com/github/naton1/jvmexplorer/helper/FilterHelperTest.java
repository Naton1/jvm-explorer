package com.github.naton1.jvmexplorer.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

class FilterHelperTest {

	@Test
	void testEmptyString() {
		final FilterHelper filterHelper = new FilterHelper();

		final Predicate<String> predicate = filterHelper.createStringPredicate("");

		Assertions.assertTrue(predicate.test("a string"));
		Assertions.assertTrue(predicate.test("another string"));
		Assertions.assertTrue(predicate.test("123abc.def.ghi.Klm"));
	}

	@Test
	void testRegexPredicate() {
		final FilterHelper filterHelper = new FilterHelper();

		final Predicate<String> predicate = filterHelper.createStringPredicate(".*");

		Assertions.assertTrue(predicate.test("a string"));
		Assertions.assertTrue(predicate.test("another string"));
		Assertions.assertTrue(predicate.test("123abc.def.ghi.Klm"));
	}

	@Test
	void testInvalidRegexPredicate() {
		// should default to contains

		final FilterHelper filterHelper = new FilterHelper();

		final Predicate<String> predicate = filterHelper.createStringPredicate(".*{");

		Assertions.assertTrue(predicate.test("string contains .*{"));
		Assertions.assertFalse(predicate.test("another string"));
		Assertions.assertFalse(predicate.test("123abc.def.ghi.Klm"));
	}

	@Test
	void testContainsPredicate() {
		final FilterHelper filterHelper = new FilterHelper();

		final Predicate<String> predicate = filterHelper.createStringPredicate("str");

		Assertions.assertTrue(predicate.test("a string"));
		Assertions.assertTrue(predicate.test("str"));
		Assertions.assertFalse(predicate.test("123abc.def.ghi.Klm"));
	}

	@Test
	void testContainsMultiPartPredicate() {
		final FilterHelper filterHelper = new FilterHelper();

		final Predicate<String> predicate = filterHelper.createStringPredicate("str,other");

		Assertions.assertTrue(predicate.test("a string"));
		Assertions.assertTrue(predicate.test("str"));
		Assertions.assertTrue(predicate.test("some.other.text"));
		Assertions.assertFalse(predicate.test("123abc.def.ghi.Klm"));
	}

}