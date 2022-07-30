package com.github.naton1.jvmexplorer.helper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class RemoteCodeTemplateHelper {

	public String load(String packageName, String className) {
		final String template = load();
		final String templateWithPackage = addPackage(template, packageName);
		final String templateWithClass = addClassName(templateWithPackage, className);
		return templateWithClass;
	}

	private String addClassName(String template, String className) {
		return replace(template, "class-name", className);
	}

	private String addPackage(String template, String packageName) {
		final String packageStatement =
				packageName != null && !packageName.isEmpty() ? ("package " + packageName + ";") : "";
		return replace(template, "package", packageStatement);
	}

	private String replace(String template, String templateKey, String replacement) {
		return template.replaceFirst("<" + templateKey + ">", replacement);
	}

	private String load() {
		try {
			final byte[] templateBytes = Objects.requireNonNull(getClass().getClassLoader()
			                                                              .getResourceAsStream(
					                                                              "remote-code-template.txt"))
			                                    .readAllBytes();
			return new String(templateBytes);
		}
		catch (IOException e) {
			log.error("Failed to load template resource", e);
			throw new IllegalStateException(e);
		}
	}

}
