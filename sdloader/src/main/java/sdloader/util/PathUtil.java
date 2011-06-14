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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Stack;

/**
 * PathUtils
 * 
 * @author c9katayama
 * 
 */
public class PathUtil {
	/**
	 * パス中の//および./および../部分を取り除きます.
	 */
	public static String narrow(String path) {
		path = replaceFileSeparator(path);

		String[] paths = path.split("/");
		Stack<String> narrowPathList = new Stack<String>();
		for (int i = 0; i < paths.length; i++) {
			if (paths[i].length() == 0 || paths[i].equals(".")) {
				continue;
			} else if (paths[i].equals("..")) {
				if (narrowPathList.size() != 0) {
					narrowPathList.pop();
				}
			} else {
				narrowPathList.add(paths[i]);
			}
		}
		boolean first = true;
		String newPath = null;
		for (String p : narrowPathList) {
			if (first) {
				newPath = path.startsWith("/") ? "/" : "";
				first = false;
			} else {
				newPath += "/";
			}
			newPath += p;
		}
		if (path.endsWith("/")) {
			newPath += "/";
		}
		return newPath;
	}

	/**
	 * ベースパスに対して相対パスを解決します。
	 * 
	 * @param basePath
	 * @param path
	 * @return
	 */
	public static String computeRelativePath(String basePath, String path) {
		basePath = basePath.substring(0, basePath.lastIndexOf("/"));
		return jointPathWithSlash(basePath, path);
	}

	/**
	 * 2つのパスを"/"で連結します。
	 */
	public static String jointPathWithSlash(String path1, String path2) {
		path1 = removeEndSlashIfNeed(path1);
		path2 = removeStartSlashIfNeed(path2);
		return path1 + "/" + path2;
	}

	public static String appendStartSlashIfNeed(final String path) {
		if (path != null && !startsWithSlash(path)) {
			return "/" + path;
		}
		return path;
	}

	public static String appendEndSlashIfNeed(final String path) {
		if (path != null && !endsWithSlash(path)) {
			return path + "/";
		}
		return path;
	}

	public static String removeStartSlashIfNeed(final String path) {
		if (path != null && startsWithSlash(path)) {
			return path.substring(1, path.length());
		}
		return path;
	}

	public static String removeEndSlashIfNeed(final String path) {
		if (path != null && path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}

	public static boolean startsWithSlash(final String path) {
		if (isEmpty(path)) {
			return false;
		}
		return path.indexOf("/") == 0;
	}

	public static boolean endsWithSlash(final String path) {
		if (isEmpty(path)) {
			return false;
		}
		return path.lastIndexOf("/") == path.length() - 1;
	}

	private static boolean isEmpty(String value) {
		return (value == null || value.trim().length() == 0);
	}

	/**
	 * パス中の\\を/に置き換えます。
	 * 
	 * @param filepath
	 * @return
	 */
	public static final String replaceFileSeparator(String path) {
		return path.replace('\\', '/');
	}

	public static final String getExtension(String path) {
		if (path == null) {
			return path;
		}
		int dot = path.lastIndexOf(".");
		if (dot == -1) {
			return null;
		}
		return path.substring(dot + 1, path.length());
	}

	/**
	 * 絶対パスかどうか
	 * 
	 * @param path
	 * @return
	 */
	public static final boolean isAbsolutePath(String path) {
		String testPath = replaceFileSeparator(path);
		if (testPath.startsWith("/") || testPath.indexOf(":") != -1) {
			return true;
		} else {
			return false;
		}
	}

	public static URL file2URL(String filePath) {
		return file2URL(new File(filePath));
	}

	public static URL file2URL(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static File url2File(String urlPath) {
		try {
			return new File(new URI(urlPath));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public static File url2File(URL url) {
		try {
			return new File(new URI(url.toExternalForm()));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
