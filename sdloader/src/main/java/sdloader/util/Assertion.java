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

/**
 * @author shot
 */
public class Assertion {

	public static <T> T notNull(T target) {
		return notNull(target, null);
	}

	public static <T> T notNull(T target, String message) {
		if (target == null) {
			throw new NullPointerException(message);
		}
		return target;
	}

	public static <T> T[] notNull(T... args) {
		for (int i = 0; i < args.length; i++) {
			notNull(args[i]);
		}
		return args;
	}

}
