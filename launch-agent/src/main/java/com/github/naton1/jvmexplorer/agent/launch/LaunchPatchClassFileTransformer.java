package com.github.naton1.jvmexplorer.agent.launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class LaunchPatchClassFileTransformer implements ClassFileTransformer {

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	                        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (className.equals("java/lang/ProcessBuilder")) {
			try {
				final ClassReader classReader = new ClassReader(classfileBuffer);
				final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
				final ClassVisitor launchPatchClassVisitor = new LaunchPatchClassVisitor(Opcodes.ASM9, classWriter);
				classReader.accept(launchPatchClassVisitor, 0);
				return classWriter.toByteArray();
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
