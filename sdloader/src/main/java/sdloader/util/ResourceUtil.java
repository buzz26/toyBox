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
/*
 * Copyright 2005-2007 the original author or authors.
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
package sdloader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import sdloader.util.databuffer.ByteDataBuffer;

/**
 * リソース用Util
 * 
 * @author c9katayama
 */
public class ResourceUtil {

	private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

	private static final String SLASH = "/";
	private static final String FILE_PROTOCOL = "file";

	public static String stripFirstProtocolPart(String path) {
		return path.substring(path.indexOf(":") + 1, path.length());
	}

	public static String stripExtension(String value) {
		int dot = value.lastIndexOf(".");
		if (dot >= 0) {
			value = value.substring(0, dot);
		}
		return value;
	}

	public static URL createURL(final String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static URL createURL(final URL baseURL, String relativeURL) {
		try {
			String protocol = baseURL.getProtocol();
			relativeURL = relativeURL.replace("\\", SLASH);
			// TODO
			if (protocol.startsWith(FILE_PROTOCOL)) {
				if (relativeURL.startsWith(SLASH)) {
					relativeURL = relativeURL
							.substring(1, relativeURL.length());
				}
				return new URL(baseURL, relativeURL);
			} else {
				// TODO
				String baseArchivePath = baseURL.toExternalForm();
				if (baseArchivePath.endsWith(SLASH)) {
					baseArchivePath = baseArchivePath.substring(0,
							baseArchivePath.length() - 1);
				}
				if (baseArchivePath.endsWith("!")) {
					baseArchivePath = baseArchivePath.substring(0,
							baseArchivePath.length() - 1);
				}
				if (!relativeURL.startsWith(SLASH)) {
					relativeURL = SLASH + relativeURL;
				}
				return new URL(baseArchivePath + "!" + relativeURL);
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isAbsoluteURL(String url) {
		return (url.indexOf(":/") != -1);
	}

	public static boolean isResourceExist(URL resource) {
		if (resource.getProtocol().equals(FILE_PROTOCOL)) {
			try {
				File file = new File(resource.toURI());
				return file.exists();
			} catch (URISyntaxException e) {
				return false;
			}
		}
		InputStream is = null;
		try {
			is = resource.openStream();
			return (is != null);
		} catch (Exception ioe) {
			return false;
		} finally {
			IOUtil.closeNoException(is);
		}
	}

	public static boolean isFileResource(URL resource) {
		return !resource.toExternalForm().endsWith(SLASH);
	}

	public static boolean isDirectoryResource(URL resource) {
		return resource.toExternalForm().endsWith(SLASH);
	}

	public static Properties loadProperties(String path, Class<?> caller) {
		InputStream is = getResourceAsStream(path, caller);
		if (is == null) {
			return null;
		}
		Properties p = new Properties();
		try {
			p.load(is);
		} catch (IOException ioe) {
			return null;
		}
		return p;
	}

	/**
	 * リソースのストリームを取得します。
	 * 
	 * @param path
	 * @param caller
	 * @return
	 */
	public static InputStream getResourceAsStream(String path, Class<?> caller) {

		String resource = path;
		if (resource.startsWith(SLASH)) {
			resource = resource.substring(1);
		}

		InputStream is = null;
		File file = new File(path);
		if (file.exists() && file.isFile()) {
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
			}
		}
		if (is == null) {
			ClassLoader tcl = Thread.currentThread().getContextClassLoader();
			if (tcl != null) {
				is = tcl.getResourceAsStream(resource);
			}
		}
		if (is == null) {
			is = caller.getResourceAsStream(path);
		}
		if (is == null) {
			is = ClassLoader.class.getResourceAsStream(path);
		}
		if (is == null) {
			is = ClassLoader.getSystemResourceAsStream(resource);
		}

		return is;
	}

	/**
	 * ストリームをコピーします。
	 * 
	 * @param in
	 * @param out
	 * @param バッファサイズ
	 * @return コピーしたバイト数
	 * @throws IOException
	 */
	public static final long copyStream(InputStream in, OutputStream out)
			throws IOException {
		return copyStream(in, out, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * ストリームをコピーします。
	 * 
	 * @param in
	 * @param out
	 * @return コピーしたバイト数
	 * @throws IOException
	 */
	public static final long copyStream(InputStream in, OutputStream out,
			int bufferSize) throws IOException {
		byte[] buf = new byte[bufferSize];
		long totalSize = 0;
		int s = -1;
		while ((s = in.read(buf)) != -1) {
			out.write(buf, 0, s);
			totalSize += s;
		}
		buf = null;
		return totalSize;
	}

	/**
	 * InputStreamからbyte[]を取得します。
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static final byte[] getBytes(InputStream is) throws IOException {
		ByteDataBuffer buf = new ByteDataBuffer();
		copyStream(is, buf.getOutputStream());
		return buf.toByteArray();
	}
}
