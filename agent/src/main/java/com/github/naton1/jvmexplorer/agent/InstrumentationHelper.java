package com.github.naton1.jvmexplorer.agent;

import com.esotericsoftware.minlog.Log;
import com.github.naton1.jvmexplorer.protocol.ClassField;
import com.github.naton1.jvmexplorer.protocol.ClassFieldKey;
import com.github.naton1.jvmexplorer.protocol.ClassFields;
import com.github.naton1.jvmexplorer.protocol.PatchResult;
import com.github.naton1.jvmexplorer.protocol.WrappedObject;
import lombok.RequiredArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class InstrumentationHelper {

	// Let's have some cutoffs for large arrays where it may overflow kryonet's buffers
	private static final int MAX_ARRAY_BYTES = 10000;
	private static final int MAX_ARRAY_STRING_LENGTH = 1000;

	private final Instrumentation instrumentation;

	// Note this includes jdk + libraries as well
	public List<Class<?>> getApplicationClasses() {
		final List<Class<?>> classes = new ArrayList<>();
		for (Class<?> c : instrumentation.getAllLoadedClasses()) {
			if (c.isArray()) {
				continue;
			}
			if (c.isPrimitive()) {
				continue;
			}
			if (!instrumentation.isModifiableClass(c)) {
				continue;
			}
			if (isAgentClass(c)) {
				continue;
			}
			classes.add(c);
		}
		return classes;
	}

	private static boolean isAgentClass(Class<?> klass) {
		final CodeSource agentCodeSource = JvmExplorerAgent.class.getProtectionDomain().getCodeSource();
		final CodeSource classCodeSource = klass.getProtectionDomain().getCodeSource();
		return agentCodeSource.getLocation() != null && classCodeSource != null && agentCodeSource.getLocation()
		                                                                                          .equals(classCodeSource.getLocation());
	}

	public byte[] getClassBytes(Class<?> klass) {
		final ClassFileSaveTransformer transformer = new ClassFileSaveTransformer(klass.getName());
		instrumentation.addTransformer(transformer, true);
		try {
			instrumentation.retransformClasses(klass);
		}
		catch (InternalError e) {
			// This provides a better error message if there is a linking problem
			try {
				klass.getDeclaredFields();
			}
			catch (NoClassDefFoundError error) {
				Log.warn("Problem linking class while retransforming", error);
				// Let's return an empty array so if we are exporting it doesn't fail.
				return new byte[0];
			}
			throw e;
		}
		catch (Exception e) {
			Log.warn("Encountered exception retransforming classes", e);
		}
		finally {
			instrumentation.removeTransformer(transformer);
		}
		return transformer.getBytes() != null ? transformer.getBytes() : new byte[0];
	}

	public boolean setObject(ClassLoader classLoader, ClassFieldKey[] classFieldKeys, Object newValue) {
		Object currentObject = null;
		try {
			for (int i = 0; i < classFieldKeys.length; i++) {
				final ClassFieldKey classFieldKey = classFieldKeys[i];
				final Class<?> currentClass = findClass(classLoader, currentObject, classFieldKey);
				if (currentClass == null) {
					Log.warn("Failed to find class for " + currentObject + " - " + classFieldKey);
					return false;
				}
				final Field field = currentClass.getDeclaredField(classFieldKey.getFieldName());
				field.setAccessible(true);
				if (i == classFieldKeys.length - 1) {
					// At the end of the path, let's set
					// Let's support changing a final value :)
					try {
						final Field modifiersField = Field.class.getDeclaredField("modifiers");
						modifiersField.setAccessible(true);
						modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
					}
					catch (NoSuchFieldException e) {
						Log.debug("Could not find modifier field; may fail to overwrite a final field", e);
					}
					field.set(currentObject, newValue);
					Log.debug("Set field " + field + " to " + newValue);
					return true;
				}
				else {
					currentObject = field.get(currentObject);
					if (currentObject == null) {
						Log.warn("Found null when searching path: " + Arrays.toString(classFieldKeys));
						return false;
					}
				}
			}
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			Log.error("Failed to set object: " + Arrays.toString(classFieldKeys), e);
		}
		return false;
	}

	private Class<?> findClass(ClassLoader classLoader, Object currentObject, ClassFieldKey classFieldKey) {
		return currentObject != null ? findClassInHierarchy(currentObject.getClass(), classFieldKey.getClassName())
		                             : getClassByName(classFieldKey.getClassName(), classLoader);
	}

	private Class<?> findClassInHierarchy(Class<?> klass, String name) {
		while (klass != null) {
			if (name.equals(klass.getName())) {
				return klass;
			}
			klass = klass.getSuperclass();
		}
		return null;
	}

	public Class<?> getClassByName(String name, ClassLoader classLoader) {
		try {
			return Class.forName(name, false, classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());
		}
		catch (ClassNotFoundException | LinkageError e) {
			Log.debug("Couldn't find class through forName: " + e);
		}
		for (Class<?> klass : instrumentation.getAllLoadedClasses()) {
			if (name.equals(klass.getName()) && (classLoader == null || klass.getClassLoader() == classLoader)) {
				return klass;
			}
		}
		Log.warn("Failed to find class by name: " + name);
		return null;
	}

	public Class<?> getClassByName(String name) {
		return getClassByName(name, null);
	}

	public ClassFields getClassFields(ClassLoader classLoader, ClassFieldKey[] classFieldPath) {
		final Object object = getObject(classLoader, classFieldPath);
		if (object == null) {
			return null;
		}
		return getClassFields(object.getClass(), object);
	}

	public Object getObject(ClassLoader classLoader, ClassFieldKey[] classFieldPath) {
		Object currentObject = null;
		try {
			for (ClassFieldKey classFieldKey : classFieldPath) {
				final Class<?> currentClass = findClass(classLoader, currentObject, classFieldKey);
				if (currentClass == null) {
					Log.warn("Failed to find class for " + currentObject + " - " + classFieldKey);
					return null;
				}
				final Field field = currentClass.getDeclaredField(classFieldKey.getFieldName());
				field.setAccessible(true);
				currentObject = field.get(currentObject);
				if (currentObject == null) {
					Log.warn("Found null when searching path: " + classFieldPath);
					return null;
				}
			}
			return currentObject;
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			Log.error("Failed to get object: " + classFieldPath, e);
			return null;
		}
	}

	public ClassFields getClassFields(Class<?> klass, Object object) {
		final List<ClassField> fields = new ArrayList<>();
		Class<?> currentClass = klass;
		try {
			while (currentClass != null) {
				final Field[] currentClassFields = currentClass.getDeclaredFields();
				for (Field field : currentClassFields) {
					if (object == null && !Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					final Object fieldValue;
					try {
						field.setAccessible(true);
						fieldValue = field.get(object);
					}
					catch (Exception ignored) {
						continue;
					}
					final ClassField classField = convertToClassField(currentClass, field, fieldValue);
					fields.add(classField);
				}
				currentClass = currentClass.getSuperclass();
			}
		}
		catch (NoClassDefFoundError e) {
			Log.warn("Problem linking class while getting fields", e);
		}
		return new ClassFields(fields.toArray(new ClassField[0]));
	}

	private ClassField convertToClassField(Class<?> currentClass, Field field, Object fieldValue) {
		final ClassFieldKey classKey = new ClassFieldKey(currentClass.getName(),
		                                                 field.getName(),
		                                                 field.getType().getName(),
		                                                 Modifier.fieldModifiers() & field.getModifiers());
		if (fieldValue instanceof Class) {
			return new ClassField(classKey, new WrappedObject(fieldValue.toString()));
		}
		if (fieldValue == null || isPrimitiveOrWrapperOrString(fieldValue)) {
			return new ClassField(classKey, fieldValue);
		}
		else if (fieldValue.getClass().isArray() && isPrimitiveOrWrapperOrString(fieldValue.getClass()
		                                                                                   .getComponentType())) {
			if (instrumentation.getObjectSize(fieldValue) >= MAX_ARRAY_BYTES) {
				return new ClassField(classKey, new WrappedObject(fieldValue.toString()));
			}
			return new ClassField(classKey, fieldValue);
		}
		else if (fieldValue.getClass().isArray()) {
			final String arrayAsString = Arrays.deepToString((Object[]) fieldValue);
			if (arrayAsString.length() < MAX_ARRAY_STRING_LENGTH) {
				return new ClassField(classKey, new WrappedObject(arrayAsString));
			}
		}
		return new ClassField(classKey, new WrappedObject(fieldValue.toString()));
	}

	private static boolean isPrimitiveOrWrapperOrString(Object object) {
		final Class<?> type = object instanceof Class<?> ? (Class<?>) object : object.getClass();
		if (type == Double.class || type == Float.class || type == Long.class || type == Integer.class
		    || type == Short.class || type == Character.class || type == Byte.class || type == Boolean.class) {
			return true;
		}
		return type.isPrimitive() || type == String.class;
	}

	public PatchResult redefineClass(Class<?> klass, byte[] bytes) {
		try {
			instrumentation.redefineClasses(new ClassDefinition(klass, bytes));
			return PatchResult.builder().success(true).build();
		}
		catch (Throwable e) {
			// Several exceptions/errors can be thrown. We just want to know if it fails.
			Log.warn("Failed to redefine class", e);
			final StringWriter stringWriter = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
			return PatchResult.builder().success(false).message(stringWriter.toString()).build();
		}
	}

}
