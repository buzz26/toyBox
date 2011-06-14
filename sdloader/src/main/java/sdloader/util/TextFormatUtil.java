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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * テキストをフォーマットします。 テキスト中の${name}の部分を、nameをキーにしてプロパティから 値を取り出し、置換します。
 * 
 * @author c9katayama
 */
public class TextFormatUtil {

	private static final Pattern REPLACE_PATTERN = Pattern
			.compile("\\$\\{[^}]*\\}");

	public static String formatTextBySystemProperties(final String text) {
		return formatTextByProperties(text, System.getProperties());
	}

	public static String formatTextByProperties(final String text,
			final Properties prop) {
		final Matcher m = REPLACE_PATTERN.matcher(text);
		final StringBuffer buf = new StringBuffer();
		while (m.find()) {
			String key = trimKey(m.group());
			String value = prop.getProperty(key);
			if (value == null)
				throw new RuntimeException("Property not found.property key="
						+ key);

			m.appendReplacement(buf, value);
		}
		m.appendTail(buf);
		return buf.toString();
	}

	private static String trimKey(String key) {
		String trimKey = key.substring(2);// &{
		return trimKey.substring(0, trimKey.length() - 1);// }
	}
}
