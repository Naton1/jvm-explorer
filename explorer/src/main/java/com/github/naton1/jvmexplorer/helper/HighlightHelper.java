package com.github.naton1.jvmexplorer.helper;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
	                                                        "try",
	                                                        "void",
	                                                        "volatile",
	                                                        "while",
	                                                        "NOP",
	                                                        "ACONST_NULL",
	                                                        "ICONST_M1",
	                                                        "ICONST_0",
	                                                        "ICONST_1",
	                                                        "ICONST_2",
	                                                        "ICONST_3",
	                                                        "ICONST_4",
	                                                        "ICONST_5",
	                                                        "LCONST_0",
	                                                        "LCONST_1",
	                                                        "FCONST_0",
	                                                        "FCONST_1",
	                                                        "FCONST_2",
	                                                        "DCONST_0",
	                                                        "DCONST_1",
	                                                        "BIPUSH",
	                                                        "SIPUSH",
	                                                        "LDC",
	                                                        "ILOAD",
	                                                        "LLOAD",
	                                                        "FLOAD",
	                                                        "DLOAD",
	                                                        "ALOAD",
	                                                        "IALOAD",
	                                                        "LALOAD",
	                                                        "FALOAD",
	                                                        "DALOAD",
	                                                        "AALOAD",
	                                                        "BALOAD",
	                                                        "CALOAD",
	                                                        "SALOAD",
	                                                        "ISTORE",
	                                                        "LSTORE",
	                                                        "FSTORE",
	                                                        "DSTORE",
	                                                        "ASTORE",
	                                                        "IASTORE",
	                                                        "LASTORE",
	                                                        "FASTORE",
	                                                        "DASTORE",
	                                                        "AASTORE",
	                                                        "BASTORE",
	                                                        "CASTORE",
	                                                        "SASTORE",
	                                                        "POP",
	                                                        "POP2",
	                                                        "DUP",
	                                                        "DUP_X1",
	                                                        "DUP_X2",
	                                                        "DUP2",
	                                                        "DUP2_X1",
	                                                        "DUP2_X2",
	                                                        "SWAP",
	                                                        "IADD",
	                                                        "LADD",
	                                                        "FADD",
	                                                        "DADD",
	                                                        "ISUB",
	                                                        "LSUB",
	                                                        "FSUB",
	                                                        "DSUB",
	                                                        "IMUL",
	                                                        "LMUL",
	                                                        "FMUL",
	                                                        "DMUL",
	                                                        "IDIV",
	                                                        "LDIV",
	                                                        "FDIV",
	                                                        "DDIV",
	                                                        "IREM",
	                                                        "LREM",
	                                                        "FREM",
	                                                        "DREM",
	                                                        "INEG",
	                                                        "LNEG",
	                                                        "FNEG",
	                                                        "DNEG",
	                                                        "ISHL",
	                                                        "LSHL",
	                                                        "ISHR",
	                                                        "LSHR",
	                                                        "IUSHR",
	                                                        "LUSHR",
	                                                        "IAND",
	                                                        "LAND",
	                                                        "IOR",
	                                                        "LOR",
	                                                        "IXOR",
	                                                        "LXOR",
	                                                        "IINC",
	                                                        "I2L",
	                                                        "I2F",
	                                                        "I2D",
	                                                        "L2I",
	                                                        "L2F",
	                                                        "L2D",
	                                                        "F2I",
	                                                        "F2L",
	                                                        "F2D",
	                                                        "D2I",
	                                                        "D2L",
	                                                        "D2F",
	                                                        "I2B",
	                                                        "I2C",
	                                                        "I2S",
	                                                        "LCMP",
	                                                        "FCMPL",
	                                                        "FCMPG",
	                                                        "DCMPL",
	                                                        "DCMPG",
	                                                        "IFEQ",
	                                                        "IFNE",
	                                                        "IFLT",
	                                                        "IFGE",
	                                                        "IFGT",
	                                                        "IFLE",
	                                                        "IF_ICMPEQ",
	                                                        "IF_ICMPNE",
	                                                        "IF_ICMPLT",
	                                                        "IF_ICMPGE",
	                                                        "IF_ICMPGT",
	                                                        "IF_ICMPLE",
	                                                        "IF_ACMPEQ",
	                                                        "IF_ACMPNE",
	                                                        "GOTO",
	                                                        "JSR",
	                                                        "RET",
	                                                        "TABLESWITCH",
	                                                        "LOOKUPSWITCH",
	                                                        "IRETURN",
	                                                        "LRETURN",
	                                                        "FRETURN",
	                                                        "DRETURN",
	                                                        "ARETURN",
	                                                        "RETURN",
	                                                        "GETSTATIC",
	                                                        "PUTSTATIC",
	                                                        "GETFIELD",
	                                                        "PUTFIELD",
	                                                        "INVOKEVIRTUAL",
	                                                        "INVOKESPECIAL",
	                                                        "INVOKESTATIC",
	                                                        "INVOKEINTERFACE",
	                                                        "INVOKEDYNAMIC",
	                                                        "NEW",
	                                                        "NEWARRAY",
	                                                        "ANEWARRAY",
	                                                        "ARRAYLENGTH",
	                                                        "ATHROW",
	                                                        "CHECKCAST",
	                                                        "INSTANCEOF",
	                                                        "MONITORENTER",
	                                                        "MONITOREXIT",
	                                                        "MULTIANEWARRAY",
	                                                        "IFNULL",
	                                                        "IFNONNULL", };

	private static final Set<String> PATTERN_GROUPS;
	private static final Pattern PATTERN;

	static {
		final Map<String, String> patternMap = new HashMap<>();
		patternMap.put("keyword", "\\b(" + String.join("|", KEYWORDS) + ")\\b");
		patternMap.put("paren", "[()]");
		patternMap.put("brace", "[{}]");
		patternMap.put("bracket", "[\\[\\]]");
		patternMap.put("semicolon", ";");
		patternMap.put("string", "\"([^\"\\\\]|\\\\.)*\"");
		patternMap.put("comment", "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/");
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
