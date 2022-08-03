package com.github.naton1.jvmexplorer.settings;

import com.github.naton1.jvmexplorer.JvmExplorer;
import com.google.gson.Gson;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.hildan.fxgson.FxGson;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Value
public class JvmExplorerSettings {

	public static final File DEFAULT_SETTINGS_FILE = new File(JvmExplorer.APP_DIR, "settings.json");

	private static final Gson GSON = FxGson.coreBuilder()
	                                       .serializeSpecialFloatingPointValues()
	                                       .setPrettyPrinting()
	                                       .create();

	private final SimpleBooleanProperty showClassLoader = new SimpleBooleanProperty();

	private final SimpleDoubleProperty firstDividerPosition = new SimpleDoubleProperty();
	private final SimpleDoubleProperty secondDividerPosition = new SimpleDoubleProperty();

	private final SimpleDoubleProperty width = new SimpleDoubleProperty(Double.NaN);
	private final SimpleDoubleProperty height = new SimpleDoubleProperty(Double.NaN);

	private final SimpleBooleanProperty maximized = new SimpleBooleanProperty(false);

	private final SimpleDoubleProperty x = new SimpleDoubleProperty(Double.NaN);
	private final SimpleDoubleProperty y = new SimpleDoubleProperty(Double.NaN);

	public static JvmExplorerSettings load(File settingsFile) {
		try {
			final String settingsFileContent = Files.readString(settingsFile.toPath());
			final JvmExplorerSettings settings = GSON.fromJson(settingsFileContent, JvmExplorerSettings.class);
			if (settings == null) {
				return new JvmExplorerSettings();
			}
			return settings;
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

	public boolean propertiesEquals(JvmExplorerSettings other) {
		final List<Property<?>> ourProperties = properties();
		final List<Property<?>> otherProperties = other.properties();
		if (ourProperties.size() != otherProperties.size()) {
			return false;
		}
		for (int i = 0; i < ourProperties.size(); i++) {
			final Object ourProperty = ourProperties.get(i).getValue();
			final Object otherProperty = otherProperties.get(i).getValue();
			if (!Objects.equals(ourProperty, otherProperty)) {
				return false;
			}
		}
		return true;
	}

	public void configureAutoSaving(File settingsFile) {
		properties().forEach(property -> property.addListener((obs, old, newv) -> save(settingsFile)));
	}

	private List<Property<?>> properties() {
		return List.of(x, y, width, height, maximized, firstDividerPosition, secondDividerPosition, showClassLoader);
	}

}
