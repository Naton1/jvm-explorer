package com.github.naton1.jvmexplorer.integration;

import com.github.naton1.jvmexplorer.settings.JvmExplorerSettings;
import com.github.naton1.jvmexplorer.settings.SettingsStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class SettingsTest {

	@Test
	void givenSettingsFile_whenSaveAndLoad_settingsAreEqual() throws IOException {
		final JvmExplorerSettings jvmExplorerSettings = JvmExplorerSettings
				.builder()
				.showClassLoader(true)
				.build();

		final File tempFile = File.createTempFile("test", "test");

		final SettingsStorage settingsStorage = new SettingsStorage();

		settingsStorage.save(tempFile, jvmExplorerSettings);
		final JvmExplorerSettings loadedSettings = settingsStorage.load(tempFile);

		Assertions.assertEquals(jvmExplorerSettings, loadedSettings);
		tempFile.delete();
	}

}
