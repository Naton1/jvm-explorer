package com.github.naton1.jvmexplorer.agent;

import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;
import org.junit.Assert;
import org.junit.Test;

public class ClassLoaderStoreTest {

	@Test
	public void givenUnknownClassDescriptor_whenLookup_thenClassLoaderIsNull() {
		final ClassLoaderStore classLoaderStore = new ClassLoaderStore();
		final ClassLoaderDescriptor classLoaderDescriptor = ClassLoaderDescriptor.builder()
		                                                                         .description("desc")
		                                                                         .id("id")
		                                                                         .build();

		final ClassLoader correspondingClassLoader = classLoaderStore.lookup(classLoaderDescriptor);

		Assert.assertNull(correspondingClassLoader);
	}

	@Test
	public void givenCachedClassloader_whenStore_thenSameDescriptorReturned() {
		final ClassLoader classLoader = getClass().getClassLoader();
		final ClassLoaderStore classLoaderStore = new ClassLoaderStore();
		final ClassLoaderDescriptor classLoaderDescriptor = classLoaderStore.store(classLoader);

		final ClassLoaderDescriptor secondClassLoaderDescriptor = classLoaderStore.store(classLoader);

		Assert.assertEquals(classLoaderDescriptor, secondClassLoaderDescriptor);
	}

	@Test
	public void givenCachedClassloader_whenLookup_thenDescriptorReturned() {
		final ClassLoader classLoader = getClass().getClassLoader();
		final ClassLoaderStore classLoaderStore = new ClassLoaderStore();
		final ClassLoaderDescriptor classLoaderDescriptor = classLoaderStore.store(classLoader);

		final ClassLoader lookedUpClassLoader = classLoaderStore.lookup(classLoaderDescriptor);

		Assert.assertEquals(classLoader, lookedUpClassLoader);
	}

	@Test
	public void givenCachedClassloader_whenClassLoaderGcdAndThenLookup_thenNoClassLoaderReturned() {
		final ClassLoader classLoader = getClass().getClassLoader();
		final ClassLoaderStore classLoaderStore = new ClassLoaderStore();
		final ClassLoaderDescriptor classLoaderDescriptor = classLoaderStore.store(classLoader);

		classLoaderStore.removeClassLoader(classLoader); // simulate gc
		final ClassLoader lookedUpClassLoader = classLoaderStore.lookup(classLoaderDescriptor);

		Assert.assertNull(lookedUpClassLoader);
	}

	@Test
	public void givenCachedClassloader_whenClassLoaderGcdAndCleanCalled_thenClassLoaderDescriptorRemoved() {
		final ClassLoader classLoader = getClass().getClassLoader();
		final ClassLoaderStore classLoaderStore = new ClassLoaderStore();
		final ClassLoaderDescriptor classLoaderDescriptor = classLoaderStore.store(classLoader);

		classLoaderStore.removeClassLoader(classLoader); // simulate gc
		classLoaderStore.clean();

		Assert.assertFalse(classLoaderStore.containsDescriptor(classLoaderDescriptor));
	}

}