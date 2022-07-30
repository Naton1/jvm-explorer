package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.kryonet.Client;
import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import com.github.naton1.jvmexplorer.protocol.ExecutionResult;
import com.github.naton1.jvmexplorer.protocol.JvmClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(MockitoJUnitRunner.class)
public class JvmConnectionImplTest {

	@Mock
	private JvmClient jvmClient;
	@Mock
	private InstrumentationHelper instrumentationHelper;
	@Mock
	private Client client;
	@Mock
	private ExecutorService executorService;
	@Mock
	private ClassLoaderStore classLoaderStore;

	@Before
	public void setup() {
		executorService = Executors.newSingleThreadExecutor();
	}

	@After
	public void teardown() {
		executorService.shutdown();
	}

	@Test
	public void testExecuteCallable() throws Exception {
		final JvmConnectionImpl jvmConnection = new JvmConnectionImpl(jvmClient,
		                                                              instrumentationHelper,
		                                                              client,
		                                                              executorService,
		                                                              classLoaderStore);

		final byte[] classBytes = getClassBytes("TestCallable.class");
		final String className = "com.github.naton1.jvmexplorer.agent.resource.TestCallable";
		final ExecutionResult executionResult = jvmConnection.executeCallable(className, classBytes, null);

		Assert.assertTrue(executionResult.getMessage(), executionResult.isSuccess());
		Assert.assertTrue(executionResult.getMessage().contains("Hello world"));
	}

	private byte[] getClassBytes(String path) throws Exception {
		try (final InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
			return read(is);
		}
	}

	private byte[] read(InputStream is) throws IOException {
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final byte[] buffer = new byte[0xFFFF];
		for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
			os.write(buffer, 0, len);
		}
		return os.toByteArray();
	}

	// Baseline test to ensure it fails normally
	@Test
	public void testExecuteCallableWithCustomClassLoaderFailure() throws Exception {
		final JvmConnectionImpl jvmConnection = new JvmConnectionImpl(jvmClient,
		                                                              instrumentationHelper,
		                                                              client,
		                                                              executorService,
		                                                              classLoaderStore);

		Mockito.when(classLoaderStore.lookup(ArgumentMatchers.<ClassLoaderDescriptor>any()))
		       .thenReturn(URLClassLoader.newInstance(new URL[0]));
		final ClassLoaderDescriptor classLoaderDescriptor = ClassLoaderDescriptor.builder().build();

		final byte[] classBytes = getClassBytes("TestCallableWhereParentIsCustomClassLoader.class");
		final String className =
				"com.github.naton1.jvmexplorer.agent.resource" + ".TestCallableWhereParentIsCustomClassLoader";
		final ExecutionResult executionResult = jvmConnection.executeCallable(className,
		                                                                      classBytes,
		                                                                      classLoaderDescriptor);

		Assert.assertFalse(executionResult.getMessage(), executionResult.isSuccess());
	}

	@Test
	public void testExecuteCallableWithCustomClassLoader() throws Exception {
		final JvmConnectionImpl jvmConnection = new JvmConnectionImpl(jvmClient,
		                                                              instrumentationHelper,
		                                                              client,
		                                                              executorService,
		                                                              classLoaderStore);

		Mockito.when(classLoaderStore.lookup(ArgumentMatchers.<ClassLoaderDescriptor>any()))
		       .thenReturn(new CustomClassLoader());
		final ClassLoaderDescriptor classLoaderDescriptor = ClassLoaderDescriptor.builder().build();

		final byte[] classBytes = getClassBytes("TestCallableWhereParentIsCustomClassLoader.class");
		final String className =
				"com.github.naton1.jvmexplorer.agent.resource" + ".TestCallableWhereParentIsCustomClassLoader";
		final ExecutionResult executionResult = jvmConnection.executeCallable(className,
		                                                                      classBytes,
		                                                                      classLoaderDescriptor);

		Assert.assertTrue(executionResult.getMessage(), executionResult.isSuccess());
		Assert.assertTrue(executionResult.getMessage().equals("true"));
	}

	private static class CustomClassLoader extends ClassLoader {

	}

}