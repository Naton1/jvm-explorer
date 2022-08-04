package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
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

	private static final HighlightContext DEFAULT_CONTEXT = of(getStaticPatterns());

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

		final String methods = createMethodPattern(classNode);
		final String fields = createFieldPattern(classNode);

		final Map<String, String> patterns = new LinkedHashMap<>(getStaticPatterns());
		patterns.put("method", methods);
		patterns.put("field", fields);

		final HighlightContext context = of(patterns);
		log.debug("Generated highlight pattern for {}: {}", classNode.name, context.getPattern().pattern());
		return context;
	}

	@Value
	public static class HighlightContext {
		private final Set<String> matchKeys;
		private final Pattern pattern;
	}

	private static String createFieldPattern(ClassNode classNode) {
		return classNode.fields.stream().map(fn -> {
			final String modifiers = Pattern.quote(Modifier.toString(Modifier.fieldModifiers() & fn.access));
			final String fieldType = getPatternForTypeDeclaration(Type.getType(fn.desc));
			final String fieldName = fn.name;
			final String fieldDefinitionHalf = Stream.of(modifiers, fieldType)
			                                         .filter(s -> !s.isEmpty())
			                                         .collect(Collectors.joining(" "));
			return "(?<=" + fieldDefinitionHalf + " )" + Pattern.quote(fieldName);
		}).collect(Collectors.joining("|"));
	}

	private static String createMethodPattern(ClassNode classNode) {
		return classNode.methods.stream().map(mn -> {
			final String modifiers = Pattern.quote(Modifier.toString(Modifier.methodModifiers() & mn.access));
			final String returnType = getPatternForTypeDeclaration(Type.getReturnType(mn.desc));
			String methodName = mn.name;
			final List<String> parts = new ArrayList<>();
			parts.add(modifiers);
			if (methodName.equals("<init>")) {
				methodName = ClassNameHelper.getSimpleName(classNode.name.replace('/', '.'));
			}
			else {
				parts.add(returnType);
			}
			final String methodDefinitionHalf = String.join(" ", parts);
			return "(?<=" + methodDefinitionHalf + " )" + Pattern.quote(methodName) + "(?=\\()";
		}).collect(Collectors.joining("|"));
	}

	// Field type, method return type
	private static String getPatternForTypeDeclaration(Type type) {
		String fieldType = ClassNameHelper.getSimpleName(type.getClassName());
		if (fieldType.contains("$")) {
			// Handle inner classes
			fieldType = fieldType.substring(fieldType.lastIndexOf("$") + 1);
		}
		// We could also handle generics by checking the signature
		return Pattern.quote(fieldType);
	}

	private static HighlightContext of(Map<String, String> patterns) {
		final Pattern pattern = compilePattern(patterns);
		return new HighlightContext(Set.copyOf(patterns.keySet()), pattern);
	}

	private static Pattern compilePattern(Map<String, String> patternMap) {
		return Pattern.compile(patternMap.entrySet()
		                                 .stream()
		                                 .map(e -> "(?<" + e.getKey() + ">" + e.getValue() + ")")
		                                 .collect(Collectors.joining("|")));
	}

	private static Map<String, String> getStaticPatterns() {
		final Map<String, String> patternMap = new LinkedHashMap<>();
		patternMap.put("keyword", "\\b(" + String.join("|", KEYWORDS) + ")\\b");
		patternMap.put("paren", "[()]");
		patternMap.put("brace", "[{}]");
		patternMap.put("bracket", "[\\[\\]]");
		patternMap.put("semicolon", ";");
		patternMap.put("string", "\"([^\"\\\\]|\\\\.)*\"");
		patternMap.put("comment", "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/");
		patternMap.put("annotation", "@[a-zA-Z\\d\\.]+");
		patternMap.put("number", "(?<=[^\\w$])\\d+");
		patternMap.put("label", "L\\d+"); // bytecode label
		patternMap.put("char", "'.+?'");
		return patternMap;
	}

}
