package com.github.naton1.jvmexplorer.integration.e2e;

import com.github.naton1.jvmexplorer.integration.helper.TestJvm;
import com.github.naton1.jvmexplorer.integration.programs.SleepForeverProgram;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Test;

@Slf4j
class ModifyMethodTest extends EndToEndTest {

	@Test
	void testInsertCodeToFront() throws Exception {
		final String codeToAdd = "System.out.println(\"TEST_STRING\");";
		testModify("testFunction", "Add", codeToAdd, codeToAdd);
	}

	@Test
	void testReplaceMethod() throws Exception {
		final String codeToAdd = "System.out.println(\"TEST_STRING\");";
		final String codeToAddWithReturn = codeToAdd + "return null;";
		testModify("testFunction", "Replace", codeToAdd, codeToAddWithReturn);
	}

	private void testModify(String method, String type, String verifyCodeContains, String codeToAdd) throws Exception {
		try (final TestJvm testJvm = TestJvm.of(SleepForeverProgram.class)) {
			appHelper.selectJvm(testJvm);
			appHelper.selectMainClass(testJvm);
			appHelper.waitUntilDecompile();

			appHelper.selectCodeAction("Modify Method");

			fxRobot.waitForStageExists("Modify Method");

			selectMethod(method);
			selectType(type);

			interact(verifyCodeContains, codeToAdd);
		}
	}

	private void interact(String verifyCodeContains, String codeToAdd) {
		final CodeArea codeArea = fxRobot.targetWindow("Modify Method").lookup("#code").queryAs(CodeArea.class);
		final String updatedCode = codeArea.getText().replaceFirst("// The.*", codeToAdd);
		fxRobot.interact(() -> codeArea.replaceText(updatedCode));

		fxRobot.interact(() -> fxRobot.targetWindow("Modify Method").lookup("Modify").queryButton().fire());

		appHelper.waitUntilJavaCodeContains(verifyCodeContains);
	}

	private void selectMethod(String text) {
		fxRobot.selectComboBox(fxRobot.lookup("#method").queryComboBox(), text);
	}

	private void selectType(String text) {
		fxRobot.selectComboBox(fxRobot.lookup("#modifyType").queryComboBox(), text);
	}

}
