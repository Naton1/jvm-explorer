package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class SettingsTest {

	@Test
	void givenSettingsFile_whenSaveAndLoad_thenSettingsAreEqual() throws IOException {
		final JvmExplorerSettings jvmExplorerSettings = new JvmExplorerSettings();
		jvmExplorerSettings.getShowClassLoader().set(true);
		jvmExplorerSettings.getFirstDividerPosition().set(0.25);
		jvmExplorerSettings.getSecondDividerPosition().set(0.5);

		final File tempFile = File.createTempFile("test", "test");

		jvmExplorerSettings.save(tempFile);
		final JvmExplorerSettings loadedSettings = JvmExplorerSettings.load(tempFile);

		Assertions.assertTrue(jvmExplorerSettings.propertiesEquals(loadedSettings));
		tempFile.delete();
	}

	@Test
	void givenNoSettingsExist_whenLoad_thenFreshSettingsGiven() throws IOException {
		final File tempFile = File.createTempFile("test", "test");

		final JvmExplorerSettings loadedSettings = JvmExplorerSettings.load(tempFile);

		Assertions.assertNotNull(loadedSettings);
		Assertions.assertTrue(new JvmExplorerSettings().propertiesEquals(loadedSettings));

		tempFile.delete();
	}

	@Test
	void givenSettingsFileWithAutoSaving_whenModify_thenSettingsSaved() throws IOException {
		final File tempFile = File.createTempFile("test", "test");
		final JvmExplorerSettings jvmExplorerSettings = new JvmExplorerSettings();
		jvmExplorerSettings.getShowClassLoader().set(true);

		final JvmExplorerSettings loadedSettings = JvmExplorerSettings.load(tempFile);
		Assertions.assertFalse(jvmExplorerSettings.propertiesEquals(loadedSettings));

		jvmExplorerSettings.configureAutoSaving(tempFile);
		jvmExplorerSettings.getFirstDividerPosition().set(0.25);
		jvmExplorerSettings.getSecondDividerPosition().set(0.5);
		final JvmExplorerSettings updatedSettings = JvmExplorerSettings.load(tempFile);

		Assertions.assertTrue(jvmExplorerSettings.propertiesEquals(updatedSettings));
		tempFile.delete();
	}

	@Test
	void testPropertiesEquals() {
		final JvmExplorerSettings jvmExplorerSettings = new JvmExplorerSettings();
		jvmExplorerSettings.getShowClassLoader().set(true);

		final JvmExplorerSettings otherSettings = new JvmExplorerSettings();
		otherSettings.getShowClassLoader().set(true);

		Assertions.assertTrue(jvmExplorerSettings.propertiesEquals(otherSettings));

		otherSettings.getFirstDividerPosition().set(0.25);
		otherSettings.getSecondDividerPosition().set(0.5);

		Assertions.assertFalse(jvmExplorerSettings.propertiesEquals(otherSettings));
	}

}
