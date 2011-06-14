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

/**
 * Webブラウザ起動用
 * 
 * @author c9katayama
 * @author yone098
 */
public class Browser {

	/**
	 * Webブラウザを起動します
	 * 
	 * @param url
	 *            接続先URL
	 */
	public static void open(final String url) {
		try {
			MiscUtils.openBrowser(url);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * 起動ブラウザを指定してWebブラウザを起動します
	 * 
	 * @param url
	 *            接続先URL
	 * @param browserPath
	 *            起動ブラウザ（フルパス）
	 */
	public static void open(final String url, final String browserPath) {
		try {
			MiscUtils.openBrowser(url, browserPath);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
