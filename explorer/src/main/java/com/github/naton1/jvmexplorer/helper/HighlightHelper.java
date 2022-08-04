package com.github.naton1.jvmexplorer.helper;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HighlightHelper {

	private static final String[] KEYWORDS = new String[] { "abstract",
	                                                        "assert",
	                                                        "boolean",
	                                                        "break",
	                                                        "byte",
	                                                        "case",
	                                                        "catch",
	                                                        "char",
	                                                        "class",
	                                                        "const",
	                                                        "continue",
	                                                        "default",
	                                                        "do",
	                                                        "double",
	                                                        "else",
	                                                        "enum",
	                                                        "extends",
	                                                        "false",
	                                                        "final",
	                                                        "finally",
	                                                        "float",
	                                                        "for",
	                                                        "goto",
	                                                        "if",
	                                                        "implements",
	                                                        "import",
	                                                        "instanceof",
	                                                        "int",
	                                                        "interface",
	                                                        "long",
	                                                        "native",
	                                                        "new",
	                                                        "package",
	                                                        "private",
	                                                        "protected",
	                                                        "public",
	                                                        "return",
	                                                        "short",
	                                                        "static",
	                                                        "strictfp",
	                                                        "super",
	                                                        "switch",
	                                                        "synchronized",
	                                                        "this",
	                                                        "throw",
	                                                        "throws",
	                                                        "transient",
	                                                        "true",
	                                                        "try",
	                                                        "void",
	                                                        "volatile",
	                                                        "while", };

	private static final Set<String> PATTERN_GROUPS;
	private static final Pattern PATTERN;

	static {
		final Map<String, String> patternMap = new LinkedHashMap<>();
		patternMap.put("keyword", "\\b(" + String.join("|", KEYWORDS) + ")\\b");
		patternMap.put("paren", "[()]");
		patternMap.put("brace", "[{}]");
		patternMap.put("bracket", "[\\[\\]]");
		patternMap.put("semicolon", ";");
		patternMap.put("string", "\"([^\"\\\\]|\\\\.)*\"");
		patternMap.put("comment", "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/");
		patternMap.put("annotation", "@[a-zA-Z\\d\\.]+");
		patternMap.put("number", "(?<=[^\\w])\\d+");
		PATTERN_GROUPS = Collections.unmodifiableSet(patternMap.keySet());
		PATTERN = Pattern.compile(patternMap.entrySet()
		                                    .stream()
		                                    .map(e -> "(?<" + e.getKey() + ">" + e.getValue() + ")")
		                                    .collect(Collectors.joining("|")));
	}

	public StyleSpans<Collection<String>> computeHighlighting(String text) {
		final Matcher matcher = PATTERN.matcher(text);
		int lastMatchEnd = 0;
		final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			final String styleClass = PATTERN_GROUPS.stream()
			                                        .filter(group -> matcher.group(group) != null)
			                                        .findFirst()
			                                        .map(String::toLowerCase)
			                                        .orElseThrow(); // There has to be a match
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastMatchEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastMatchEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastMatchEnd);
		return spansBuilder.create();
	}

}
