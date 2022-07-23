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

		Assertions.assertTrue(checkEqual(jvmExplorerSettings, loadedSettings));
		tempFile.delete();
	}

	@Test
	void givenNoSettingsExist_whenLoad_thenFreshSettingsGiven() throws IOException {
		final File tempFile = File.createTempFile("test", "test");

		final JvmExplorerSettings loadedSettings = JvmExplorerSettings.load(tempFile);

		Assertions.assertNotNull(loadedSettings);
		Assertions.assertTrue(checkEqual(new JvmExplorerSettings(), loadedSettings));

		tempFile.delete();
	}

	private boolean checkEqual(JvmExplorerSettings explorerSettings, JvmExplorerSettings actualSettings) {
		if (explorerSettings.getShowClassLoader().get() != actualSettings.getShowClassLoader().get()) {
			return false;
		}
		return true;
	}

}
