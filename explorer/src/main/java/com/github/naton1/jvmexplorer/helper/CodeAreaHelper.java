package com.github.naton1.jvmexplorer.helper;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
@Slf4j
public class CodeAreaHelper {

	private final HighlightHelper highlightHelper = new HighlightHelper();
	private final ExecutorService executorService;

	// CodeArea must be in a VBox to replace and insert into VirtualizedScrollPane
	public void initializeJavaEditor(CodeArea codeArea) {
		codeArea.getStylesheets()
		        .setAll(Objects.requireNonNull(getClass().getClassLoader().getResource("css/code-area.css"))
		                       .toExternalForm());
		// Hack to insert the scroll pane. SceneBuilder wasn't picking it up.
		final VBox parent = (VBox) codeArea.getParent();
		parent.getChildren().remove(codeArea);
		final Node scrollPane = new VirtualizedScrollPane<>(codeArea);
		parent.getChildren().add(scrollPane);
		VBox.setVgrow(scrollPane, Priority.ALWAYS);
		codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
		codeArea.multiPlainChanges()
		        .successionEnds(Duration.ofMillis(500))
		        .retainLatestUntilLater(executorService)
		        .supplyTask(() -> computeHighlighting(codeArea))
		        .awaitLatest(codeArea.multiPlainChanges())
		        .filterMap(t -> {
			        if (t.isSuccess()) {
				        return Optional.of(t.get());
			        }
			        else {
				        log.warn("Failed to compute highlighting", t.getFailure());
				        return Optional.empty();
			        }
		        })
		        .subscribe(highlighting -> applyHighlighting(codeArea, highlighting));
	}

	public void triggerHighlightUpdate(CodeArea codeArea) {
		final Task<StyleSpans<Collection<String>>> initialTask = computeHighlighting(codeArea);
		initialTask.setOnSucceeded(e -> applyHighlighting(codeArea, initialTask.getValue()));
	}

	private Task<StyleSpans<Collection<String>>> computeHighlighting(CodeArea codeArea) {
		final String text = codeArea.getText();
		final Task<StyleSpans<Collection<String>>> task = new Task<>() {
			@Override
			protected StyleSpans<Collection<String>> call() {
				return highlightHelper.computeHighlighting(text);
			}
		};
		executorService.execute(task);
		return task;
	}

	private void applyHighlighting(CodeArea codeArea, StyleSpans<Collection<String>> highlighting) {
		codeArea.setStyleSpans(0, highlighting);
	}

}
