package com.github.naton1.jvmexplorer.helper;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class ClipboardHelper {

	public static void copy(String text) {
		final Clipboard clipboard = Clipboard.getSystemClipboard();
		final ClipboardContent content = new ClipboardContent();
		content.putString(text);
		clipboard.setContent(content);
	}

}
