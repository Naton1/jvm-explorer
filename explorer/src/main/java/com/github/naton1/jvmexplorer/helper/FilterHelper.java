package com.github.naton1.jvmexplorer.helper;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class FilterHelper {

	public Predicate<String> createStringPredicate(String text) {
		final String searchPattern = createSearchPattern(text);
		final Pattern pattern = Pattern.compile(searchPattern);
		return string -> pattern.matcher(string).find();
	}

	private String createSearchPattern(String text) {
		if (text.isEmpty()) {
			return ".*";
		}
		final Pattern classNamePattern = Pattern.compile("[\\p{L}\\p{N}_$.,]*");
		if (!classNamePattern.matcher(text).matches()) {
			try {
				Pattern.compile(text);
				// Text is valid regex
				return text;
			}
			catch (PatternSyntaxException ignored) {
			}
		}
		final String[] pieces = text.split(",");
		return "(?:" + Arrays.stream(pieces).map(Pattern::quote).collect(Collectors.joining("|")) + ")";
	}

}
