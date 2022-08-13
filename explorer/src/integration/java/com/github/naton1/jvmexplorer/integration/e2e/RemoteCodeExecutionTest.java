package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class RemoteCodeExecutionTest extends EndToEndTest {

	@Test
	void testExecuteRemoteCode() throws Exception {
		testExecuteCode(null, null);
	}

	@Test
	void testExecuteRemoteCodeWithFunction() throws Exception {
		final String code = "if (true) return SleepForeverProgram.testFunction(\"test-input\");";
		testExecuteCode(code, "test-input");
	}

	private void testExecuteCode(String inputCodeOverride, String expectedOutputOverride) throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.selectClassAction("Run Code In Package");

			fxRobot.waitForStageExists("Remote Code Executor.*");

			if (inputCodeOverride != null) {
				final CodeArea codeArea = fxRobot.targetWindow("Remote Code Executor.*")
				                                 .lookup("#code")
				                                 .queryAs(CodeArea.class);
				final String updatedCode = codeArea.getText().replaceFirst("//.*", inputCodeOverride);
				fxRobot.interact(() -> codeArea.replaceText(updatedCode));
			}

			fxRobot.interact(() -> fxRobot.targetWindow("Remote Code Executor.*")
			                              .lookup("Execute Code")
			                              .queryButton()
			                              .fire());

			final TextInputControl textArea = fxRobot.targetWindow("Remote Code Executor.*")
			                                         .lookup(TextArea.class::isInstance)
			                                         .queryTextInputControl();

			fxRobot.waitUntil(() -> textArea.getText().contains("Succeeded"), 5000);

			if (expectedOutputOverride == null) {
				expectedOutputOverride = "Hello world from " + testJvm.getProcess().pid();
			}
			Assertions.assertTrue(textArea.getText().contains(expectedOutputOverride), textArea.getText());

			log.info("Output text: {}", textArea.getText());
		}
	}

}
