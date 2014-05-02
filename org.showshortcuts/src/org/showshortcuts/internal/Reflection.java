package org.showshortcuts.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Assert;

/**
 * Simple reflection helper. Can be used to invoke methods and query field
 * values more conveniently than with <code>java.lang.reflect</code>.
 *
 * @author d031150
 */
public final class Reflection {

	/**
	 * Creates an instance for the given object
	 *
	 * @param o
	 *            the object. Must not be <code>null</code>
	 * @return an instance
	 */
	public static Reflection forObject(Object o) {
		return new Reflection(o);
	}

	/**
	 * Returns whether the class with the given name can be loaded
	 *
	 * @param className
	 *            the class name
	 * @param classLoader
	 *            the class loader, usually
	 *            <code>getClass().getClassLoader()</code>
	 * @return <code>true</code> if the class can be loaded, <code>false</code>
	 *         otherwise
	 *
	 * @see #classForName(String, ClassLoader)
	 */
	public static boolean classIsAvailable(String className, ClassLoader classLoader) {
		return classForName(className, classLoader) != null;
	}

	/**
	 * Loads the class with the given name and loader
	 *
	 * @param className
	 *            the class name
	 * @param classLoader
	 *            the class loader, usually
	 *            <code>getClass().getClassLoader()</code>
	 * @return the class or <code>null</code>
	 *
	 * @see #classIsAvailable(String, ClassLoader)
	 */
	public static Class<?> classForName(String className, ClassLoader classLoader) {
		try {
			return Class.forName(className, true, classLoader);
		} catch (ClassNotFoundException e) { //NOPMD
			return null;
		} catch (LinkageError e) { //NOPMD
			Activator.log(e);
			return null;
		}
	}

	/**
	 * Creates an instance for a new object of the given class
	 * 
	 * @param clazz
	 *            the class
	 * @param argTypes
	 *            the constructor argument types
	 * @param args
	 *            the constructor arguments
	 * 
	 * @return an instance
	 */
	public static Reflection forNewObject(Class<?> clazz, Class<?>[] argTypes, Object... args) {
		try {
			Constructor<?> constructor = clazz.getConstructor(argTypes);
			Object instance = constructor.newInstance(args);
			return forObject(instance);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw new IllegalStateException(cause.getMessage(), cause);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private final Object object;

	private Reflection(Object object) {
		this.object = object;
	}

	/**
	 * Returns whether a method with the given name exists on this object's
	 * class or on one of its super classes or interfaces.
	 *
	 * @param methodName
	 *            the method name
	 * @return whether the method exists
	 *
	 * @see #supportsMethod(String, Class[])
	 */
	public boolean supportsMethod(String methodName) {
		return supportsMethod(methodName, (Class<?>[]) null);
	}

	/**
	 * Returns whether a method with the given name exists on this object's
	 * class or on one of its super classes or interfaces.
	 *
	 * @param methodName
	 *            the method name
	 * @param argTypes
	 *            the argument types. May be <code>null</code>.
	 * @return whether the method exists
	 */
	public boolean supportsMethod(String methodName, Class<?>... argTypes) {
		return getMethod(methodName, argTypes) != null;
	}

	/**
	 * Invokes a method with the given name on this object
	 *
	 * @param methodName
	 *            the method name
	 * @return the method's result. Returns <code>null</code> if the method
	 *         returns <code>null</code> or if the method doesn't exist.
	 */
	public Object invoke(String methodName) {
		return invoke(methodName, null, (Object[]) null);
	}

	/**
	 * Invokes a method with the given name on this object
	 *
	 * @param methodName
	 *            the method name
	 * @param argTypes
	 *            the argument types or <code>null</code>
	 * @param args
	 *            the arguments or <code>null</code>
	 * @return the method's result. Returns <code>null</code> if the method
	 *         returns <code>null</code> or if the method doesn't exist.
	 */
	public Object invoke(String methodName, Class<?>[] argTypes, Object... args) {
		try {
			Method method = getMethod(methodName, argTypes);
			if (method == null) {
				return null;
			}
			method.setAccessible(true);
			return method.invoke(this.object, args);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw new IllegalStateException(cause.getMessage(), cause);
		}
	}

	/**
	 * Returns the value of the field with the given name on the given class
	 *
	 * @param fieldName
	 *            the field name
	 * @param clazz
	 *            the class that declares the field
	 * @return the field value. Returns <code>null</code> if the field value is
	 *         <code>null</code> or if the field doesn't exist.
	 */
	public Object getFieldValue(String fieldName, Class<?> clazz) {
		try {
			Class<?> cls = clazz != null ? clazz : this.object.getClass();
			Field field = getField(fieldName, cls);
			if (field != null) {
				field.setAccessible(true);
				return field.get(this.object);
			}
			return null;
		} catch (SecurityException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	/**
	 * Set the value of the field with the given name on the object this class
	 * was {@link #forObject(Object) instantiated} with.
	 *
	 * @param fieldName
	 *            the field name
	 * @param clazz
	 *            the class that declares the field
	 * @param value
	 *            the value to set
	 */
	public void setFieldValue(String fieldName, Class<?> clazz, Object value) {
		try {
			Class<?> cls = clazz != null ? clazz : this.object.getClass();
			Field field = getField(fieldName, cls);
			if (field != null) {
				field.setAccessible(true);
				field.set(this.object, value);
			}
		} catch (SecurityException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	/**
	 * Returns whether the field with given name exists on the given class
	 *
	 * @param fieldName
	 *            the field name
	 * @param clazz
	 *            the class that declares the field
	 * @return <code>true</code> if the field is declared on the class,
	 *         <code>false</code> otherwise
	 */
	public static boolean hasField(String fieldName, Class<?> clazz) {
		return getField(fieldName, clazz) != null;
	}

	private Method getMethod(String methodName, Class<?>... argTypes) {
		Method method = getPublicMethod(methodName, argTypes);
		if (method != null) {
			return method;
		}
		return getDeclaredMethod(methodName, argTypes);
	}

	private Method getPublicMethod(String methodName, Class<?>... argTypes) {
		if (this.object == null) {
			return null;
		}
		try {
			return this.object.getClass().getMethod(methodName, argTypes);
		} catch (SecurityException e) { //NOPMD
		} catch (NoSuchMethodException e) { //NOPMD
		}
		return null;
	}

	private Method getDeclaredMethod(String methodName, Class<?>... paramTypes) { //NOPMD
		if (this.object == null) {
			return null;
		}
		return getDeclaredMethod(this.object.getClass(), methodName, paramTypes);
	}

	private static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
		try {
			return clazz.getDeclaredMethod(name, paramTypes);
		} catch (NoSuchMethodException e) { //NOPMD
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null) {
				return getDeclaredMethod(superClass, name, paramTypes);
			}
		} catch (SecurityException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return null;
	}

	private static Field getField(String fieldName, Class<?> clazz) {
		Assert.isNotNull(clazz, "class must not be null"); //$NON-NLS-1$

		try {
			return clazz.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e) { //NOPMD
			return null;
		} catch (SecurityException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

}
