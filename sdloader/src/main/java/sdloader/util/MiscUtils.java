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

import java.io.IOException;

import sdloader.exception.NotImplementedYetException;

/**
 * 
 * @author c9katayama
 * @author yone098
 */
public class MiscUtils {

	private static String[] browserNames = { "firefox", "mozilla-firefox",
			"mozilla", "konqueror", "netscape", "opera" };

	public static void openBrowser(String url) throws IOException {
		final String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("windows") != -1) {
			url = appendUrlForWindows2000(os, url);
			Runtime.getRuntime().exec(
					"rundll32 url.dll,FileProtocolHandler " + url);
			return;
		} else if (os.indexOf("mac") != -1) {
			Runtime.getRuntime().exec(new String[] { "open", url });
			return;
		} else {
			Runtime runtime = Runtime.getRuntime();
			for (int i = 0; i < browserNames.length; i++) {
				try {
					runtime.exec(new String[] { browserNames[i], url });
					return;
				} catch (Exception e) {
				}
			}
		}
		throw new NotImplementedYetException();
	}

	/**
	 * 起動ブラウザを指定してブラウザを起動します
	 * 
	 * @param url
	 *            接続先URL
	 * @param browserPath
	 *            起動ブラウザパス
	 * @throws IOException
	 */
	public static void openBrowser(String url, final String browserPath)
			throws IOException {
		final String os = System.getProperty("os.name").toLowerCase();
		if (os.indexOf("windows") != -1) {
			url = appendUrlForWindows2000(os, url);
		}
		Runtime.getRuntime().exec(new String[] { browserPath, url });
	}

	/**
	 * // 2000の場合、htmlもしくはhtmで終わるとブラウザが開かない為、ダミーの#をつける
	 * 
	 * @param os
	 *            OS名
	 * @param url
	 *            接続URL
	 * @return URL文字列
	 */
	private static String appendUrlForWindows2000(final String os,
			final String url) {
		String ret = url;
		if (os.indexOf("2000") != -1) {
			if (url.toLowerCase().endsWith(".html")
					|| url.toLowerCase().endsWith(".htm")) {
				ret += "#";
			}
		}
		return ret;
	}

}
