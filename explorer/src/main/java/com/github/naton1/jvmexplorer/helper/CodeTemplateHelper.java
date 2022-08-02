package com.github.naton1.jvmexplorer.helper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class CodeTemplateHelper {

	public String loadModifyMethod(String className, String methodDescription, String code) {
		final String template = loadTemplate("modify-method-template.txt");
		return template.replace("<class-name>", className)
		               .replace("<method-desc>", methodDescription)
		               .replace("<code>", code)
		               .strip();
	}

	public String loadRemoteCallable(String packageName, String className) {
		final String template = loadTemplate("remote-code-template.txt");
		final String templateWithPackage = addPackage(template, packageName);
		final String templateWithClass = addClassName(templateWithPackage, className);
		return templateWithClass.strip();
	}

	private String loadTemplate(String path) {
		try {
			final byte[] templateBytes = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path))
			                                    .readAllBytes();
			return new String(templateBytes);
		}
		catch (IOException e) {
			log.error("Failed to load template resource {}", path, e);
			throw new IllegalStateException(e);
		}
	}

	private String addPackage(String template, String packageName) {
		final String packageStatement =
				packageName != null && !packageName.isEmpty() ? ("package " + packageName + ";") : "";
		return replace(template, "package", packageStatement);
	}

	private String addClassName(String template, String className) {
		return replace(template, "class-name", className);
	}

	private String replace(String template, String templateKey, String replacement) {
		return template.replaceFirst("<" + templateKey + ">", replacement);
	}

}
