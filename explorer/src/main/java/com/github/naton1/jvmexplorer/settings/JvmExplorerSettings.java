package com.github.naton1.jvmexplorer.settings;

import com.github.naton1.jvmexplorer.JvmExplorer;
import com.google.gson.Gson;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hildan.fxgson.FxGson;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

@Getter
@Slf4j
public class JvmExplorerSettings {

	public static final File DEFAULT_SETTINGS_FILE = new File(JvmExplorer.APP_DIR, "settings.json");

	private static final Gson GSON = FxGson.coreBuilder().setPrettyPrinting().create();

	private final SimpleBooleanProperty showClassLoader = new SimpleBooleanProperty();

	public static JvmExplorerSettings load(File settingsFile) {
		try {
			final String settingsFileContent = Files.readString(settingsFile.toPath());
			return GSON.fromJson(settingsFileContent, JvmExplorerSettings.class);
		}
		catch (FileNotFoundException | NoSuchFileException e) {
			return new JvmExplorerSettings();
		}
		catch (Exception e) {
			log.warn("Failed to load initial settings", e);
			return new JvmExplorerSettings();
		}
	}

	public void save(File settingsFile) {
		try {
			final File parent = settingsFile.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			final String settingsFileContent = GSON.toJson(this);
			Files.writeString(settingsFile.toPath(), settingsFileContent);
		}
		catch (Exception e) {
			log.warn("Failed to save settings", e);
		}
	}

}
