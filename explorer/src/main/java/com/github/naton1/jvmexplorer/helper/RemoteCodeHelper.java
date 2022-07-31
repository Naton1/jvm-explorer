package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.fx.compile.RemoteCodeExecutorController;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.LoadedClass;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RemoteCodeHelper {

	public void showExecuteCode(Window owner, List<LoadedClass> classpath, ClassLoaderDescriptor classLoaderDescriptor,
	                            String packageName, ExecutorService executorService, ClientHandler clientHandler,
	                            ObjectProperty<RunningJvm> currentJvm) {
		try {
			final Dialog<?> dialog = new Dialog<>();
			final FXMLLoader loader = new FXMLLoader(getClass().getClassLoader()
			                                                   .getResource("fxml/remote_code_executor.fxml"));
			final Parent root = loader.load();
			final RemoteCodeExecutorController remoteCodeExecutorController = loader.getController();
			remoteCodeExecutorController.initialize(executorService,
			                                        clientHandler,
			                                        currentJvm.get(),
			                                        classLoaderDescriptor,
			                                        packageName,
			                                        classpath);
			dialog.getDialogPane().setContent(root);
			dialog.getDialogPane().getButtonTypes().setAll();
			dialog.getDialogPane().getStyleClass().add("custom-dialog-pane");
			final String title = Stream.of("Remote Code Executor", classLoaderDescriptor, packageName)
			                           .filter(Objects::nonNull)
			                           .map(Object::toString)
			                           .map(String::trim)
			                           .filter(s -> !s.isEmpty())
			                           .collect(Collectors.joining(" - "));
			dialog.setTitle(title);
			dialog.initOwner(owner);
			dialog.initModality(Modality.NONE);
			dialog.setResizable(true);
			final Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
			final ChangeListener<RunningJvm> changeListener = (obs, old, newv) -> dialogWindow.hide();
			dialog.setOnHidden(e -> currentJvm.removeListener(changeListener));
			currentJvm.addListener(changeListener);
			dialogWindow.setOnCloseRequest(e -> dialog.hide());
			dialog.show();
		}
		catch (IOException e) {
			log.warn("Failed to initialize code executor", e);
		}
	}

}
