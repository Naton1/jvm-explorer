package com.github.naton1.jvmexplorer.agent;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

@RequiredArgsConstructor
public class ClassFileSaveTransformer implements ClassFileTransformer {

	private final String className;

	@Getter
	private byte[] bytes;

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	                        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (className.replace('/', '.').equals(this.className)) {
			bytes = classfileBuffer;
		}
		return null;
	}

}
