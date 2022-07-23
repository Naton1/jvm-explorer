package com.github.naton1.jvmexplorer.agent;

import com.github.naton1.jvmexplorer.protocol.ClassLoaderDescriptor;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class ClassLoaderStore {

	// We need two maps here (inverses of one another).
	// One to store descriptors, and one to look back up the corresponding classloader.
	private final Map<ClassLoader, ClassLoaderDescriptor> classLoaderDescriptors = new WeakHashMap<>();
	private final Map<ClassLoaderDescriptor, WeakReference<ClassLoader>> classLoaders = new HashMap<>();

	public synchronized ClassLoaderDescriptor store(ClassLoader classLoader) {
		final ClassLoaderDescriptor savedClassLoaderDescriptor = classLoaderDescriptors.get(classLoader);
		if (savedClassLoaderDescriptor != null) {
			return savedClassLoaderDescriptor;
		}
		final ClassLoaderDescriptor parentDescriptor =
				classLoader.getParent() != null ? store(classLoader.getParent()) : null;
		final ClassLoaderDescriptor newClassLoaderDescriptor = ClassLoaderDescriptor.builder()
		                                                                            .id(UUID.randomUUID().toString())
		                                                                            .description(classLoader.toString())
		                                                                            .simpleClassName(classLoader.getClass()
		                                                                                                        .getSimpleName())
		                                                                            .parent(parentDescriptor)
		                                                                            .build();
		classLoaderDescriptors.put(classLoader, newClassLoaderDescriptor);
		classLoaders.put(newClassLoaderDescriptor, new WeakReference<>(classLoader));
		return newClassLoaderDescriptor;
	}

	public synchronized ClassLoader lookup(ClassLoaderDescriptor classLoaderDescriptor) {
		if (classLoaderDescriptor == null) {
			// Bootstrap classloader
			return null;
		}
		final WeakReference<ClassLoader> classLoaderRef = classLoaders.get(classLoaderDescriptor);
		if (classLoaderRef == null) {
			return null;
		}
		final ClassLoader classLoader = classLoaderRef.get();
		if (classLoader == null) {
			classLoaders.remove(classLoaderDescriptor);
			return null;
		}
		return classLoader;
	}

	// This exists solely to clean up a bit of extra memory. It won't break anything but let's not leave
	// dead entries in the map.
	public synchronized void clean() {
		final Set<ClassLoaderDescriptor> toRemove = new HashSet<>();
		for (Map.Entry<ClassLoaderDescriptor, WeakReference<ClassLoader>> entry : classLoaders.entrySet()) {
			final WeakReference<ClassLoader> classLoaderRef = entry.getValue();
			if (classLoaderRef.get() == null) {
				toRemove.add(entry.getKey());
			}
		}
		for (ClassLoaderDescriptor classLoaderDescriptor : toRemove) {
			classLoaders.remove(classLoaderDescriptor);
		}
	}

	// Exposed for testing, simulates a garbage collection
	synchronized void removeClassLoader(ClassLoader classLoader) {
		final ClassLoaderDescriptor classLoaderDescriptor = classLoaderDescriptors.remove(classLoader);
		if (classLoaderDescriptor == null) {
			return;
		}
		final WeakReference<ClassLoader> classLoaderRef = classLoaders.get(classLoaderDescriptor);
		classLoaderRef.clear();
	}

	// Exposed for testing, to ensure clean works
	synchronized boolean containsDescriptor(ClassLoaderDescriptor classLoaderDescriptor) {
		return classLoaders.containsKey(classLoaderDescriptor);
	}

}
