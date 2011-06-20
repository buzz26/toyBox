/*
 * Copyright 2005-2008 the original author or authors.
 *
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
package sdloader.desktopswt.util;

import java.util.regex.Pattern;

/**
 * Boolean utility
 * 
 * @author c9katayama
 */
public class BooleanUtil {

	public static final Pattern YES_PATTERN = Pattern.compile("(yes|true|y)",
			Pattern.CASE_INSENSITIVE);

	public static boolean toBoolean(String s) {
		if (s == null || "".equals(s)) {
			return false;
		}
		return YES_PATTERN.matcher(s).matches();
	}
}
