package com.github.naton1.jvmexplorer.helper;

import com.github.naton1.jvmexplorer.JarTestHelper;
import com.github.naton1.jvmexplorer.Startup;
import com.github.naton1.jvmexplorer.agent.RunningJvm;
import com.github.naton1.jvmexplorer.net.ClientHandler;
import com.github.naton1.jvmexplorer.protocol.PatchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class PatchHelperTest {

	private static final RunningJvm JVM = new RunningJvm("id", "name");

	@Mock
	private ClientHandler clientHandler;

	@Test
	void testPatch() throws Exception {
		final PatchHelper patchHelper = new PatchHelper();

		final File jarFile = JarTestHelper.buildJar(Startup.class);
		final AtomicInteger patchedClasses = new AtomicInteger();

		Mockito.when(clientHandler.replaceClass(ArgumentMatchers.any(), ArgumentMatchers.any(),
		                                        ArgumentMatchers.any()))
		       .thenReturn(PatchResult.builder().success(true).build());

		final boolean success = patchHelper.patch(jarFile, JVM, clientHandler, null, patchedClasses::set);

		Assertions.assertTrue(success);
		Assertions.assertEquals(1, patchedClasses.get());

		Mockito.verify(clientHandler, Mockito.times(patchedClasses.get()))
		       .replaceClass(ArgumentMatchers.eq(JVM), ArgumentMatchers.any(), ArgumentMatchers.any());
	}

}