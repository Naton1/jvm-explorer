package com.github.naton1.jvmexplorer.helper;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HighlightHelper {

	private static final HighlightContext DEFAULT_CONTEXT =
			HighlightPatterns.of(HighlightPatterns.getStaticPatterns());

	public static StyleSpans<Collection<String>> computeHighlighting(String text) {
		return computeHighlighting(text, DEFAULT_CONTEXT);
	}

	public static StyleSpans<Collection<String>> computeHighlighting(String text, HighlightContext highlightContext) {
		final Matcher matcher = highlightContext.getPattern().matcher(text);
		int lastMatchEnd = 0;
		final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			final String styleClass = highlightContext.getMatchKeys()
			                                          .stream()
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

	// Some of this context is considering how the decompiler will decompile the code
	public static HighlightContext createContextFor(ClassNode classNode) {

		final String methods = HighlightPatterns.createMethodPattern(classNode);
		final String fields = HighlightPatterns.createFieldPattern(classNode);

		final Map<String, String> patterns = new LinkedHashMap<>(HighlightPatterns.getStaticPatterns());
		patterns.put("method", methods);
		patterns.put("field", fields);

		final HighlightContext context = HighlightPatterns.of(patterns);
		log.debug("Generated highlight pattern for {}: {}", classNode.name, context.getPattern().pattern());
		return context;
	}

	@Value
	public static class HighlightContext {
		private final Set<String> matchKeys;
		private final Pattern pattern;
	}

}
