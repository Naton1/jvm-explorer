package com.github.naton1.jvmexplorer.fx.compile;

import com.github.naton1.jvmexplorer.agent.AgentException;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.bytecode.compile.CompileResult;
import com.github.naton1.jvmexplorer.bytecode.compile.Compiler;
import com.github.naton1.jvmexplorer.bytecode.compile.JavacBytecodeProvider;
import com.github.naton1.jvmexplorer.bytecode.compile.RemoteJavacBytecodeProvider;
import com.github.naton1.jvmexplorer.helper.CodeAreaHelper;
import com.github.naton1.jvmexplorer.helper.RemoteCodeTemplateHelper;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.ExecutionResult;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
public class RemoteCodeExecutorController {

	private static final String CLASS_NAME = "RemoteTask";

	private final RemoteCodeTemplateHelper remoteCodeTemplateHelper = new RemoteCodeTemplateHelper();

	@FXML
	private CodeArea code;

	@FXML
	private TextArea output;

	@FXML
	private Button runButton;

	private ExecutorService executorService;
	private ClientHandler clientHandler;
	private ClassLoaderDescriptor classLoaderDescriptor;
	private RunningJvm runningJvm;
	private List<LoadedClass> classpath;

	private String mainClassName;

	public void initialize(ExecutorService executorService, ClientHandler clientHandler, RunningJvm runningJvm,
	                       ClassLoaderDescriptor classLoaderDescriptor, String packageName,
	                       List<LoadedClass> classpath) {
		this.executorService = executorService;
		this.clientHandler = clientHandler;
		this.runningJvm = runningJvm;
		this.classLoaderDescriptor = classLoaderDescriptor;
		this.classpath = classpath;
		this.mainClassName = (packageName != null && !packageName.isEmpty() ? (packageName + ".") : "") + CLASS_NAME;

		final CodeAreaHelper codeAreaHelper = new CodeAreaHelper(executorService);

		final String codeTemplate = remoteCodeTemplateHelper.load(packageName, CLASS_NAME)
		                                                    .strip(); // remove initial lines for package
		codeAreaHelper.initializeJavaEditor(code);
		code.replaceText(codeTemplate);
		codeAreaHelper.triggerHighlightUpdate(code);

		setupContextMenu();
	}

	@FXML
	void onExecute() {
		output.setText("Compiling...");
		log.debug("Compiling class with {} classes on classpath", classpath.size());
		executorService.submit(() -> {
			final Compiler compiler = new Compiler();
			final JavacBytecodeProvider javacBytecodeProvider = new RemoteJavacBytecodeProvider(clientHandler,
			                                                                                    runningJvm,
			                                                                                    classpath);
			final int targetJavaVersion = Math.min(getJavaVersion(), Runtime.version().feature());
			final CompileResult compileResult = compiler.compile(targetJavaVersion,
			                                                     mainClassName,
			                                                     code.getText(),
			                                                     javacBytecodeProvider);
			log.debug("Compile result: {}", compileResult);
			if (!compileResult.isSuccess()) {
				Platform.runLater(() -> setOutputText("Compilation Failed", compileResult.getStdOut()));
				return;
			}
			Platform.runLater(() -> output.setText("Executing code..."));
			log.debug("Executing code in class loader: {}", classLoaderDescriptor);
			final ExecutionResult result = clientHandler.executeCallable(runningJvm,
			                                                             mainClassName,
			                                                             compileResult.getClassContent(),
			                                                             classLoaderDescriptor);
			Platform.runLater(() -> setOutputText("Execution " + (result.isSuccess() ? "Succeeded" : "Failed"),
			                                      result.getMessage()));
		});
	}

	private void setupContextMenu() {
		final ContextMenu contextMenu = new ContextMenu();

		final MenuItem run = new MenuItem("Execute Code");
		run.setOnAction(e -> runButton.fire());

		final KeyCodeCombination shortcut = new KeyCodeCombination(KeyCode.R, KeyCodeCombination.CONTROL_DOWN);
		run.setAccelerator(shortcut);

		// It seems like menu item accelerators don't trigger in the CodeArea. We have to manually wire it together.
		final InputMap<KeyEvent> inputMap = InputMap.consume(EventPattern.keyPressed(shortcut),
		                                                     keyEvent -> run.fire());
		Nodes.addInputMap(code, inputMap);

		contextMenu.getItems().add(run);

		code.setContextMenu(contextMenu);
	}

	private void setOutputText(String header, String body) {
		output.setText(header + System.lineSeparator() + System.lineSeparator() + body);
	}

	private int getJavaVersion() {
		try {
			String version = runningJvm.getSystemProperties().getProperty("java.version");
			if (version.startsWith("1.")) {
				version = version.substring(2, 3);
			}
			else {
				final int dot = version.indexOf(".");
				if (dot != -1) {
					version = version.substring(0, dot);
				}
			}
			return Integer.parseInt(version);
		}
		catch (AgentException e) {
			log.warn("Failed to get java version for remote code execution", e);
			return Runtime.version().feature();
		}
	}

}
