package com.github.naton1.jvmexplorer.helper;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.reactfx.Subscription;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Slf4j
public class CodeAreaHelper {

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
		final Subscription subscription = codeArea.multiPlainChanges()
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
		                                          .subscribe(highlighting -> applyHighlighting(codeArea,
		                                                                                       highlighting));

		// Stop highlighting when the window is closed
		final EventHandler<WindowEvent> eventHandler = e -> {
			log.debug("Window closed, unsubscribing highlighting for {}", codeArea);
			subscription.unsubscribe();
		};
		codeArea.sceneProperty().addListener((obs, old, newv) -> {
			if (old != null) {
				old.getWindow().removeEventHandler(WindowEvent.WINDOW_HIDDEN, eventHandler);
			}
			if (newv != null) {
				newv.getWindow().addEventHandler(WindowEvent.WINDOW_HIDDEN, eventHandler);
			}
		});

		// Auto-indent to the whitespace of the previous line
		final Pattern whiteSpace = Pattern.compile("^\\s+");
		codeArea.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
			if (keyEvent.getCode() == KeyCode.ENTER) {
				final int caretPosition = codeArea.getCaretPosition();
				final int currentParagraph = codeArea.getCurrentParagraph();
				final Matcher matcher = whiteSpace.matcher(codeArea.getParagraph(currentParagraph - 1)
				                                                   .getSegments()
				                                                   .get(0));
				if (matcher.find()) {
					Platform.runLater(() -> codeArea.insertText(caretPosition, matcher.group()));
				}
			}
		});
	}

	private Task<HighlightContainer> computeHighlighting(CodeArea codeArea) {
		final String text = codeArea.getText();
		final String propName = HighlightHelper.HighlightContext.class.getName();
		final var highlightContext = (HighlightHelper.HighlightContext) codeArea.getProperties().get(propName);
		final Task<HighlightContainer> task = new Task<>() {
			@Override
			protected HighlightContainer call() {
				final StyleSpans<Collection<String>> highlighting;
				if (highlightContext != null) {
					highlighting = HighlightHelper.computeHighlighting(text, highlightContext);
				}
				else {
					highlighting = HighlightHelper.computeHighlighting(text);
				}
				return new HighlightContainer(highlighting, text);
			}
		};
		executorService.execute(task);
		return task;
	}

	private void applyHighlighting(CodeArea codeArea, HighlightContainer highlighting) {
		if (!codeArea.getText().equals(highlighting.getText())) {
			log.debug("Detected text change; aborting highlight change");
			return;
		}
		codeArea.setStyleSpans(0, highlighting.getHightlighting());
	}

	public void triggerHighlightUpdate(CodeArea codeArea) {
		final Task<HighlightContainer> initialTask = computeHighlighting(codeArea);
		initialTask.setOnSucceeded(e -> applyHighlighting(codeArea, initialTask.getValue()));
	}

	// Prevent highlighting the wrong text if the text changes
	@Value
	private static class HighlightContainer {
		private final StyleSpans<Collection<String>> hightlighting;
		private final String text;
	}

}
