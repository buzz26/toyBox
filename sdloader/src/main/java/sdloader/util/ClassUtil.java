/*
 * Copyright 2005-2010 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author shot
 */
public class ClassUtil {

	public static <T> Class<T> forName(final String className) {
		final String name = Assertion.notNull(className);
		final ClassLoader loader = Assertion.notNull(Thread.currentThread()
				.getContextClassLoader());
		return forName(name, loader);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> forName(final String className,
			final ClassLoader loader) {
		try {
			return (Class<T>) Class.forName(className, true, loader);
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> Class<T> forNameNoException(final String className) {
		final String name = Assertion.notNull(className);
		final ClassLoader loader = Assertion.notNull(Thread.currentThread()
				.getContextClassLoader());
		return forNameNoException(name, loader);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> forNameNoException(final String className,
			final ClassLoader loader) {
		try {
			return (Class<T>) Class.forName(className, true, loader);
		} catch (final Throwable ignore) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(final String clazzName) {
		return (T) newInstance(forName(clazzName, Thread.currentThread()
				.getContextClassLoader()));
	}

	@SuppressWarnings("unchecked")
	public static <T> T newInstance(final String clazzName, ClassLoader cl) {
		return (T) newInstance(forName(clazzName, cl));
	}

	public static <T> T newInstance(final Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (final InstantiationException e) {
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T newInstance(final Constructor<T> constructor,
			final Object... args) {
		try {
			return constructor.newInstance(args);
		} catch (final InstantiationException e) {
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (final InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isAssignableFrom(Class<?> from, Class<?> to) {
		final boolean fromPrimitive = from.isPrimitive();
		if (to == Object.class && !fromPrimitive) {
			return true;
		}
		final boolean toPrimitive = to.isPrimitive();
		if (!fromPrimitive && toPrimitive || fromPrimitive && !toPrimitive) {
			return false;
		}
		return to.isAssignableFrom(from);
	}

	public static boolean hasClass(final String className) {
		if (className == null) {
			return false;
		}
		try {
			final Class<?> c = forName(className);
			return c != null;
		} catch (Throwable t) {
			return false;
		}
	}

	public static Method getMethod(Class<?> target, final String methodName,
			final Class<?>[] params) {
		try {
			return target.getDeclaredMethod(methodName, params);
		} catch (Exception e) {
			target = target.getSuperclass();
			if (target != null) {
				return getMethod(target, methodName, params);
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	public static Method getMethodNoException(Class<?> target,
			final String methodName, final Class<?>[] params) {
		try {
			return target.getDeclaredMethod(methodName, params);
		} catch (Exception e) {
			target = target.getSuperclass();
			if (target != null) {
				return getMethodNoException(target, methodName, params);
			} else {
				return null;
			}
		}
	}

	public static Object invoke(final Object target, final Method method,
			final Object[] params) {
		try {
			method.setAccessible(true);
			return method.invoke(target, params);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
