package com.github.naton1.jvmexplorer.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

@Slf4j
public class SettingsStorage {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public void save(File settingsFile, JvmExplorerSettings jvmExplorerSettings) {
		try {
			final String settingsFileContent = GSON.toJson(jvmExplorerSettings);
			Files.writeString(settingsFile.toPath(), settingsFileContent);
		}
		catch (Exception e) {
			log.warn("Failed to save settings", e);
		}
	}

	public JvmExplorerSettings load(File settingsFile) {
		try {
			final String settingsFileContent = Files.readString(settingsFile.toPath());
			return GSON.fromJson(settingsFileContent, JvmExplorerSettings.class);
		}
		catch (FileNotFoundException | NoSuchFileException e) {
			return JvmExplorerSettings.builder().build();
		}
		catch (Exception e) {
			log.warn("Failed to load initial settings", e);
			return JvmExplorerSettings.builder().build();
		}
	}

}
