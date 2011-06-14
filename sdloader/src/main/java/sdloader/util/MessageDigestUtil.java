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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * メッセージダイジェスト用Util
 * 
 * @author shot
 * @author c9katayama
 */
public class MessageDigestUtil {

	protected static String ALGORHTYM = "MD5";

	/**
	 * 引数の文字列を元にダイジェストした文字列を返します。
	 * 
	 * @param planeText
	 * @return
	 */
	public static String digest(String planeText) {
		MessageDigest digest = createMessageDigest();

		byte[] b = planeText.getBytes();
		String hex = toHexString(digest.digest(b));
		return hex;
	}

	/**
	 * byte[]を16進文字列に変換します
	 * 
	 * @param buf
	 * @return
	 */
	public static String toHexString(byte[] buf) {
		String digestText = "";
		for (int i = 0; i < buf.length; i++) {
			int n = buf[i] & 0xff;
			if (n < 16) {
				digestText += "0";
			}
			digestText += Integer.toHexString(n).toUpperCase();
		}
		return digestText;
	}

	/**
	 * MessageDisgestを作成します。
	 * 
	 * @return
	 */
	public static MessageDigest createMessageDigest() {
		try {
			return MessageDigest.getInstance(ALGORHTYM);
		} catch (NoSuchAlgorithmException e) {
			throw new ExceptionInInitializerError();
		}
	}

	public static void setAlgorithm(String algorithm) {
		ALGORHTYM = algorithm;
	}

	public static void resetAlgorithm() {
		ALGORHTYM = "MD5";
	}
}
