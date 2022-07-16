package com.github.naton1.jvmexplorer.agent.launch;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class LaunchPatchClassVisitor extends ClassVisitor {

	public LaunchPatchClassVisitor(int api, ClassVisitor classVisitor) {
		super(api, classVisitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
		if (name.equals("start") && desc.equals("()Ljava/lang/Process;")) {
			methodVisitor = new LaunchPatchMethodVisitor(api, methodVisitor, access, name, desc);
		}
		return methodVisitor;
	}

	private static class LaunchPatchMethodVisitor extends AdviceAdapter {

		public LaunchPatchMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name,
		                                String descriptor) {
			super(api, methodVisitor, access, name, descriptor);
		}

		@Override
		public void onMethodEnter() {
			final URL runningJarUrl = LaunchPatchAgent.class.getProtectionDomain().getCodeSource().getLocation();
			final String agentJarPath = new File(URI.create(runningJarUrl.toString())).getAbsolutePath();

			// Verify the command is a java command
			// !command.isEmpty() && command.get(0).matches(".*java(?:.exe)?")
			visitVarInsn(ALOAD, 0);
			visitFieldInsn(GETFIELD, "java/lang/ProcessBuilder", "command", "Ljava/util/List;");
			visitMethodInsn(INVOKEINTERFACE, "java/util/List", "isEmpty", "()Z", true);

			final Label label1 = new Label();
			visitJumpInsn(IFNE, label1);
			visitVarInsn(ALOAD, 0);
			visitFieldInsn(GETFIELD, "java/lang/ProcessBuilder", "command", "Ljava/util/List;");
			visitInsn(ICONST_0);
			visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
			visitTypeInsn(CHECKCAST, "java/lang/String");
			visitLdcInsn(".*java(?:.exe)?");
			visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "matches", "(Ljava/lang/String;)Z", false);
			visitJumpInsn(IFEQ, label1);

			// Add java agent to index 1
			// command.add(1, "-javaagent:" + agentJarPath);
			visitVarInsn(ALOAD, 0);
			visitFieldInsn(GETFIELD, "java/lang/ProcessBuilder", "command", "Ljava/util/List;");
			visitInsn(ICONST_1);
			visitLdcInsn("-javaagent:" + agentJarPath);
			visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(ILjava/lang/Object;)V", true);

			visitLabel(label1);

			// Remove any DisableAttachMechanism
			// command.remove("-XX:+DisableAttachMechanism");
			visitVarInsn(ALOAD, 0);
			visitFieldInsn(GETFIELD, "java/lang/ProcessBuilder", "command", "Ljava/util/List;");
			visitLdcInsn("-XX:+DisableAttachMechanism");
			visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
			visitInsn(POP);
		}

	}

}
