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

		final File tempFile = File.createTempFile("test", "test");

		jvmExplorerSettings.save(tempFile);
		final JvmExplorerSettings loadedSettings = JvmExplorerSettings.load(tempFile);

		Assertions.assertEquals(jvmExplorerSettings, loadedSettings);
		tempFile.delete();
	}

}
