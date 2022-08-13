package com.github.naton1.jvmexplorer.integration.helper;

import com.github.naton1.jvmexplorer.fx.classes.FilterableTreeItem;
import com.github.naton1.jvmexplorer.helper.FileHelper;
import com.github.naton1.jvmexplorer.protocol.helper.ClassNameHelper;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeView;
import lombok.RequiredArgsConstructor;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.UnaryOperator;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

@RequiredArgsConstructor
public class AppHelper {

	private final FxRobotPlus fxRobot;

	public ListView<?> getJvmList() {
		return fxRobot.lookup("#processes").queryListView();
	}

	public void selectJvm(TestJvm testJvm) {
		selectJvm(testJvm.getProcess().pid() + "");
	}

	public void selectJvm(String jvmName) {
		fxRobot.waitUntil(() -> fxRobot.select(getJvmList(), jvmName), 5000);
	}

	public TreeView<?> getClassTree() {
		return fxRobot.lookup("#classes").queryAs(TreeView.class);
	}

	public void selectClass(String className) {
		fxRobot.waitUntil(() -> fxRobot.select(getClassTree(), className), 5000);
	}

	public void selectMainClass(TestJvm testJvm) {
		final String simpleName = ClassNameHelper.getSimpleName(testJvm.getMainClassName());
		selectClass(simpleName);
	}

	public TreeView<?> getFieldTree() {
		return fxRobot.lookup("#classFields").queryAs(TreeView.class);
	}

	public void waitUntilDecompile() {
		final String className = getClassTree().getSelectionModel().getSelectedItem().getValue().toString();
		waitUntilJavaCodeContains("class " + className);
	}

	public void waitUntilDisassemble() {
		final CodeArea byteCode = getByteCode();
		final String className = getClassTree().getSelectionModel().getSelectedItem().getValue().toString();
		fxRobot.waitUntil(() -> byteCode.getText().contains("class " + className), 5000);
	}

	public TextInputControl getSearchClasses() {
		return fxRobot.lookup("#searchClasses").queryTextInputControl();
	}

	public Stream<String> streamClassTree() {
		final TreeView<?> treeView = getClassTree();
		return ((FilterableTreeItem<?>) treeView.getRoot()).streamVisible().map(Object::toString);
	}

	public void selectClassAction(String action) {
		fxRobot.selectContextMenu(getClassTree(), action);
	}

	public void waitForClassesToLoad() {
		fxRobot.waitUntil(() -> streamClassTree().findAny().isPresent(), 5000);
	}

	public void waitForClassesToBeEmpty() {
		fxRobot.waitUntil(() -> streamClassTree().findAny().isEmpty(), 5000);
	}

	public void searchClasses(String text) {
		fxRobot.interact(() -> getSearchClasses().setText(text));
	}

	public void setTestFile(File file) {
		FileHelper.setTestFile(fxRobot.window("JVM Explorer.*"), file);
	}

	public File createTempJar() {
		try {
			return File.createTempFile("test", ".jar");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public File createTempClass() {
		try {
			return File.createTempFile("test", ".class");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void disableClassLoaders() {
		if (isClassLoadersVisible()) {
			// Disable initially
			selectClassAction("Show Class Loaders");
			waitForClassesToLoad();
			fxRobot.waitUntil(() -> !isClassLoadersVisible(), 5000);
		}
	}

	public void enableClassLoaders() {
		if (!isClassLoadersVisible()) {
			// Disable initially
			selectClassAction("Show Class Loaders");
			waitForClassesToLoad();
			fxRobot.waitUntil(this::isClassLoadersVisible, 5000);
		}
	}

	public boolean isClassLoadersVisible() {
		return streamClassTree().anyMatch(s -> s.equals("PlatformClassLoader"));
	}

	public Stream<String> streamClasses(File jarFile) {
		try {
			return new JarFile(jarFile).stream().filter(j -> !j.isDirectory()).map(ZipEntry::getName);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void waitForAlert(String titleRegex, String containsText) {
		fxRobot.waitUntil((Runnable) () -> fxRobot.targetWindow(titleRegex).lookup(containsText).query(), 5000);
	}

	public boolean isTabChangesPending(int index) {
		final TabPane tabPane = getTabPane();
		return tabPane.getTabs().get(index).getText().endsWith("*");
	}

	public void waitUntilChangesNotPending(int index) {
		fxRobot.waitUntil(() -> !isTabChangesPending(index), 5000);
	}

	public void patchCode(UnaryOperator<String> patcher) {
		final CodeArea classFile = getJavaCode();
		final String updatedText = patcher.apply(classFile.getText());
		fxRobot.interact(() -> classFile.replaceText(updatedText));
	}

	public void patchBytecode(UnaryOperator<String> patcher) {
		final CodeArea byteCode = getByteCode();
		final String updatedText = patcher.apply(byteCode.getText());
		fxRobot.interact(() -> byteCode.replaceText(updatedText));
	}

	public CodeArea getJavaCode() {
		return fxRobot.lookup("#classFile").queryAs(CodeArea.class);
	}

	public CodeArea getByteCode() {
		return fxRobot.lookup("#bytecode").queryAs(CodeArea.class);
	}

	public void saveCodeChanges() {
		selectCodeAction("Save Changes");
	}

	public void saveBytecodeChanges() {
		final CodeArea classFile = getByteCode();
		fxRobot.selectContextMenu(classFile.getContextMenu(), "Save Changes");
	}

	public void resetCodeChanges() {
		selectCodeAction("Reset Changes");
	}

	public void resetBytecodeChanges() {
		final CodeArea classFile = getByteCode();
		fxRobot.selectContextMenu(classFile.getContextMenu(), "Reset Changes");
	}

	public void selectCodeAction(String action) {
		final CodeArea classFile = getJavaCode();
		fxRobot.selectContextMenu(classFile.getContextMenu(), action);
	}

	public TabPane getTabPane() {
		return fxRobot.lookup("#currentClassTabPane").queryAs(TabPane.class);
	}

	public void openTab(int index) {
		final TabPane tabPane = getTabPane();
		tabPane.getSelectionModel().select(index);
	}

	public void waitUntilJavaCodeContains(String text) {
		fxRobot.waitUntil(() -> getJavaCode().getText().contains(text), 5000);
	}

	public void waitUntilByteCodeContains(String text) {
		fxRobot.waitUntil(() -> getByteCode().getText().contains(text), 5000);
	}

}
