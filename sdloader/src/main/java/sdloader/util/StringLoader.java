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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;

/**
 * StringLoader is an utility class to get messages from properties file. This
 * class is intended to use loading properties statically, and also is not
 * intend to be used multiple locale environments which message by locale is
 * dynamically switched by each user request.
 * 
 * @author shot
 */
public class StringLoader {

	protected static SDLoaderLog logger = SDLoaderLogFactory
			.getLog(StringLoader.class);

	public static void load(Class<?> holder) {
		load(holder, holder.getName());
	}

	public static void load(Class<?> holder, String name) {
		ResourceBundle rb = getBundle(name, holder.getClassLoader());
		if (rb == null) {
			return;
		}
		Field[] fields = holder.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			if (validateMask(field)) {
				continue;
			}
			String key = field.getName();
			if (key == null) {
				return;
			}
			String msg = rb.getString(key);
			try {
				if (isAssignableFrom(String.class, field)) {
					field.set(null, msg);
				}
			} catch (Exception e) {
				logger.info(e.getMessage() + e);
			}
		}
	}

	private static boolean validateMask(Field f) {
		final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
		final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
		return (f.getModifiers() & MOD_MASK) != MOD_EXPECTED;
	}

	private static ResourceBundle getBundle(String name, ClassLoader loader) {
		try {
			return ResourceBundle.getBundle(name, Locale.getDefault(), loader);
		} catch (MissingResourceException e) {
			return null;
		}
	}

	private static boolean isAssignableFrom(final Class<?> clazz,
			final Field target) {
		return clazz.isAssignableFrom(target.getType());
	}
}
