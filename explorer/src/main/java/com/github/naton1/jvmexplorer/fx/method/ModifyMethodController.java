package com.github.naton1.jvmexplorer.fx.method;

import com.github.naton1.jvmexplorer.agent.AgentException;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.bytecode.AsmDisassembler;
import com.github.naton1.jvmexplorer.bytecode.Disassembler;
import com.github.naton1.jvmexplorer.bytecode.compile.CompileResult;
import com.github.naton1.jvmexplorer.bytecode.compile.Compiler;
import com.github.naton1.jvmexplorer.bytecode.compile.JavacBytecodeProvider;
import com.github.naton1.jvmexplorer.bytecode.compile.RemoteJavacBytecodeProvider;
import com.github.naton1.jvmexplorer.helper.AsmHelper;
import com.github.naton1.jvmexplorer.helper.CodeAreaHelper;
import com.github.naton1.jvmexplorer.helper.CodeTemplateHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import com.github.naton1.jvmexplorer.protocol.PatchResult;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class ModifyMethodController {

	private static final String CLASS_NAME = "MethodModification";
	private static final String METHOD_NAME = "onMethodCall";

	@FXML
	private CodeArea code;

	@FXML
	private TextArea output;

	@FXML
	private ComboBox<ModifyType> modifyType;

	@FXML
	private ComboBox<MethodDescriptor> method;

	private ExecutorService executorService;
	private ClientHandler clientHandler;
	private RunningJvm runningJvm;
	private LoadedClass initialClass;
	private List<LoadedClass> classpath;
	private Runnable onClose;
	private byte[] classFile;

	private ClassNode classNode;

	// Prevent garbage collection
	// Note if the controller is gc'd then the template is gc'd too
	private StringBinding template;

	public void initialize(ExecutorService executorService, ClientHandler clientHandler, RunningJvm runningJvm,
	                       LoadedClass initialClass, Runnable onClose, List<LoadedClass> classpath, byte[] classFile) {
		this.executorService = executorService;
		this.clientHandler = clientHandler;
		this.runningJvm = runningJvm;
		this.classpath = classpath;
		this.initialClass = initialClass;
		this.onClose = onClose;
		this.classFile = classFile;

		final CodeAreaHelper codeAreaHelper = new CodeAreaHelper(executorService);

		modifyType.getItems().setAll(ModifyType.values());
		modifyType.getSelectionModel().selectFirst();

		resetClassNode();

		template = Bindings.createStringBinding(() -> buildBaseCode(method.getValue(), modifyType.getValue()),
		                                        modifyType.valueProperty(),
		                                        method.valueProperty());

		final ChangeListener<String> bindingListener = (obs, old, newv) -> {
			code.replaceText(newv);
			codeAreaHelper.triggerHighlightUpdate(code);
		};
		template.addListener(bindingListener);
		bindingListener.changed(template, null, template.getValue());

		codeAreaHelper.initializeJavaEditor(code);
	}

	private void resetClassNode() {
		// We modify the ClassNode methods in-place after compiling.
		// Therefore, if there is some failure, we need to reset the class node, so it doesn't stay corrupted.
		classNode = AsmHelper.parse(classFile);
		final List<MethodDescriptor> methods = classNode.methods.stream()
		                                                        .map(MethodDescriptor::new)
		                                                        .collect(Collectors.toList());
		final int selectedIndex = method.getSelectionModel().getSelectedIndex();
		method.getItems().setAll(methods);
		if (selectedIndex == -1) {
			// Initial setup
			method.getSelectionModel().selectFirst();
		}
		else {
			// Select same method as before, after resetting
			method.getSelectionModel().select(selectedIndex);
		}
	}

	@FXML
	void onCompile() {
		onCompile(c -> Platform.runLater(() -> setOutputText("Compiled Successfully", c.getStdOut())));
	}

	@FXML
	void onModify() {
		final MethodDescriptor selectedMethod = method.getValue();
		final ModifyType selectedModifyType = modifyType.getValue();
		onCompile(compileResult -> {
			try {
				postProcess(compileResult, selectedMethod, selectedModifyType);
			}
			catch (Exception e) {
				log.warn("Failed to post-process method", e);
				Platform.runLater(() -> {
					setOutputText("Class Processor Failed", e.getMessage());
					resetClassNode();
				});
				return;
			}

			// We can use the stack frames from the valid bytecode we already have
			final byte[] finishedClass = AsmHelper.parse(ClassWriter.COMPUTE_MAXS, classNode);

			final PatchResult patchResult = clientHandler.replaceClass(runningJvm, initialClass, finishedClass);
			if (!patchResult.isSuccess()) {
				final Disassembler disassembler = new AsmDisassembler();
				final String disassembledClass = disassembler.process(finishedClass);
				log.debug("Failed to patch class\n{}\n{}", patchResult.getMessage(), disassembledClass);
				Platform.runLater(() -> {
					setOutputText("Patch Failed", patchResult.getMessage());
					resetClassNode();
				});
				return;
			}
			log.debug("Patched class successfully");
			Platform.runLater(this::onCancel); // close
		});
	}

	@FXML
	void onCancel() {
		onClose.run();
	}

	private void postProcess(CompileResult compileResult, MethodDescriptor selectedMethod,
	                         ModifyType selectedModifyType) {
		final ClassNode compiledClass = AsmHelper.parse(compileResult.getClassContent());
		final MethodNode updatedMethod = compiledClass.methods.stream()
		                                                      .filter(mn -> mn.name.equals(METHOD_NAME))
		                                                      .findFirst()
		                                                      .orElseThrow();
		final MethodNode methodToModify = classNode.methods.stream()
		                                                   .filter(mn -> mn == selectedMethod.getMethodNode())
		                                                   .findFirst()
		                                                   .orElseThrow();

		switch (selectedModifyType) {
		case ADD_BEFORE:
			// Patch returns to continue to the actual method
			final Label after = new Label();
			replaceReturn(updatedMethod, after);
			final AbstractInsnNode frame = new FrameNode(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
			methodToModify.instructions.insert(new LabelNode(after));
			methodToModify.instructions.insert(frame);
			methodToModify.instructions.insert(updatedMethod.instructions);
			// new instructions -> label -> new frame -> old instructions
			break;
		case REPLACE:
			methodToModify.instructions.clear();
			methodToModify.instructions.add(updatedMethod.instructions);
			break;
		}
		delegateCalls(classNode, methodToModify);
	}

	private void onCompile(Consumer<CompileResult> onCompilation) {
		setOutputText("Compiling...", "Please wait.");
		// I love when java can't compile my lambda without casting
		executorService.submit(() -> {
			log.debug("Compiling class with {} classes on classpath", classpath.size());
			final Compiler compiler = new Compiler();
			final JavacBytecodeProvider javacBytecodeProvider = new RemoteJavacBytecodeProvider(clientHandler,
			                                                                                    runningJvm,
			                                                                                    classpath);
			final int targetJavaVersion = Math.min(getJavaVersion(), Runtime.version().feature());
			final CompileResult compileResult = compiler.compile(targetJavaVersion,
			                                                     CLASS_NAME,
			                                                     code.getText(),
			                                                     javacBytecodeProvider);
			log.debug("Compile result: {}", compileResult);
			if (!compileResult.isSuccess()) {
				Platform.runLater(() -> setOutputText("Compilation Failed", compileResult.getStdOut()));
				return;
			}
			onCompilation.accept(compileResult);
		});
	}

	private String buildBaseCode(MethodDescriptor methodDesc, ModifyType modifyType) {
		final String returnType =
				modifyType.isExpectsReturnValue() ? Type.getReturnType(methodDesc.getMethodNode().desc).getClassName()
				                                  : "void";
		final String method = methodDesc.buildTemplate(METHOD_NAME, returnType);
		final CodeTemplateHelper codeTemplateHelper = new CodeTemplateHelper();
		final String code = modifyType.getComment();
		return codeTemplateHelper.loadModifyMethod(CLASS_NAME, method, code);
	}

	private void replaceReturn(MethodNode updatedMethod, Label label) {
		final List<AbstractInsnNode> returns = new ArrayList<>();
		for (AbstractInsnNode insn : updatedMethod.instructions) {
			if (insn.getOpcode() == Opcodes.RETURN) {
				returns.add(insn);
			}
		}
		returns.forEach(ret -> {
			final AbstractInsnNode jump = new JumpInsnNode(Opcodes.GOTO, new LabelNode(label));
			updatedMethod.instructions.set(ret, jump);
		});
	}

	private void delegateCalls(ClassNode owner, MethodNode updatedMethod) {
		// Delegate calls to the real class, not the class we compiled against
		updatedMethod.instructions.forEach(insn -> {
			if (insn instanceof FieldInsnNode) {
				final FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;
				if (fieldInsnNode.owner.equals(CLASS_NAME)) {
					fieldInsnNode.owner = owner.name;
				}
			}
			else if (insn instanceof MethodInsnNode) {
				final MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
				if (methodInsnNode.owner.equals(CLASS_NAME)) {
					methodInsnNode.owner = owner.name;
				}
			}
		});
	}

	private int getJavaVersion() {
		try {
			return runningJvm.getJavaVersion();
		}
		catch (AgentException e) {
			log.warn("Failed to get java version for remote code execution", e);
			return Runtime.version().feature();
		}
	}

	private void setOutputText(String header, String body) {
		output.setText(header + System.lineSeparator() + System.lineSeparator() + body);
	}

	@RequiredArgsConstructor
	private enum ModifyType {
		ADD_BEFORE("Add Code Before Method",
		           false,
		           "// The following method body will be called before the rest of the method"),
		REPLACE("Replace Method Body", true, "// The following method body will replace the specified method body");
		private final String description;
		@Getter
		private final boolean expectsReturnValue;
		@Getter
		private final String comment;

		@Override
		public String toString() {
			return description;
		}
	}

	@Value
	private static class MethodDescriptor {
		private final MethodNode methodNode;

		public String buildTemplate(String methodName, String returnType) {
			final AtomicInteger paramIndex = new AtomicInteger(0);
			final String arguments = Arrays.stream(Type.getArgumentTypes(methodNode.desc))
			                               .map(Type::getClassName)
			                               .map(type -> type + " var" + paramIndex.getAndIncrement())
			                               .collect(Collectors.joining(", "));
			final boolean isStatic = Modifier.isStatic(methodNode.access);
			final String methodPrefix = "public " + (isStatic ? "static " : "");
			return methodPrefix + returnType + " " + methodName + "(" + arguments + ")";
		}

		@Override
		public String toString() {
			final String returnType = Type.getReturnType(methodNode.desc).getClassName();
			return buildTemplate(methodNode.name, returnType);
		}
	}

}
