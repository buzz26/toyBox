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
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Web用ユーティリティ
 * 
 * @author c9katayama
 */
@SuppressWarnings("unchecked")
public class WebUtil {
	/**
	 * ヘッダー日付フォーマット
	 */
	private static final DateFormat HEADER_DATE_FORMAT = new SimpleDateFormat(
			"E, d MMM yyyy HH:mm:ss 'GMT'", Locale.UK);
	/**
	 * クッキー日付フォーマット
	 */
	private static final DateFormat COOKIE_DATE_FORMAT = new SimpleDateFormat(
			"E, d-MMM-yyyy HH:mm:ss 'GMT'", Locale.UK);

	private WebUtil() {
		super();
	}

	/**
	 * ContentType(text/html;charse=UTF-8など)からUTF-8部分を取り出します。
	 * 
	 * @param value
	 * @return
	 */
	public static String parseCharsetFromContentType(String value) {
		final String sepString = "charset=";
		int sep = value.indexOf(sepString);
		if (sep != -1) {
			sep += sepString.length();
			int endIndex = value.length();
			int scIndex = value.indexOf(";", sep);
			if (scIndex != -1)
				endIndex = scIndex;
			String charSet = value.substring(sep, endIndex);
			return charSet.trim();
		} else {
			return null;
		}
	}

	/**
	 * サポートされているエンコードかどうか サポートされていない場合、UnsupportedEncodingExceptionが発生します。
	 * 
	 * @param encoding
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static void checkSupportedEndcoding(String encoding)
			throws UnsupportedEncodingException {
		if (encoding == null)
			throw new UnsupportedCharsetException("Charset is null.");
		URLDecoder.decode("", encoding);
	}

	/**
	 * ヘッダー用の日付文字を、ミリ秒フォーマットに変換します。
	 * 
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static long parseHeaderDate(String date) throws ParseException {
		synchronized (HEADER_DATE_FORMAT) {
			return HEADER_DATE_FORMAT.parse(date).getTime();
		}
	}

	/**
	 * クッキー用の日付文字を、ミリ秒フォーマットに変換します。
	 * 
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static long parseCookieDate(String date) throws ParseException {
		synchronized (COOKIE_DATE_FORMAT) {
			return COOKIE_DATE_FORMAT.parse(date).getTime();
		}
	}

	/**
	 * 日付をヘッダー用の日付文字列にフォーマットします。
	 * 
	 * @param date
	 * @return
	 */
	public static String formatHeaderDate(Date date) {
		synchronized (HEADER_DATE_FORMAT) {
			return HEADER_DATE_FORMAT.format(date);
		}
	}

	/**
	 * 日付をクッキー用の日付文字列にフォーマットします。
	 * 
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static String formatCookieDate(Date date) {
		synchronized (COOKIE_DATE_FORMAT) {
			return COOKIE_DATE_FORMAT.format(date);
		}
	}

	public static URL[] createClassPaths(String targetDir,
			FileFilter fileFilter, boolean recursive) {
		return createClassPaths(new File(targetDir), fileFilter, recursive);
	}

	/**
	 * 対象ディレクトリ中のファイルへのURLを生成します。
	 * 
	 * @param targetDir
	 * @param fileFilter
	 * @param recursive
	 *            再帰的に追加するかどうか
	 * @return
	 * @throws MalformedURLException
	 */
	public static URL[] createClassPaths(File targetDir, FileFilter fileFilter,
			boolean recursive) {

		if (!targetDir.exists())
			return null;

		List<URL> urlList = CollectionsUtil.newArrayList();

		File[] libs = targetDir.listFiles(fileFilter);
		if (libs != null) {
			for (int i = 0; i < libs.length; i++) {
				urlList.add(PathUtil.file2URL(libs[i]));
			}
		}
		if (recursive) {
			File[] dirs = targetDir.listFiles();
			if (dirs != null) {
				for (int i = 0; i < dirs.length; i++) {
					if (dirs[i].isDirectory()) {
						URL[] urls = createClassPaths(
								dirs[i].getAbsolutePath(), fileFilter,
								recursive);
						if (urls != null) {
							for (int j = 0; j < urls.length; j++)
								urlList.add(urls[j]);
						}
					}
				}
			}
		}
		return (URL[]) urlList.toArray(new URL[] {});
	}

	/**
	 * リクエストURI中のコンテキストパス以外の部分を返します。 resoucePath = servletpath + pathinfo
	 * ただし、requestURIが"/"の場合(ルートアプリケーション）のみ、"/"を返します。
	 * 
	 * @param requestURI
	 * @return resourcePath
	 */
	public static String getResourcePath(String contextPath, String requestURI) {
		if (requestURI == null)
			return null;
		if (requestURI.equals("/")) {
			return "/";
		}
		if (contextPath.equals(requestURI)) {
			return null;
		} else {
			String servletPath = requestURI.substring(contextPath.length(),
					requestURI.length());
			return servletPath;
		}
	}

	/**
	 * servletPath+PathInfoのパスから、サーブレットパスの部分を返します。 resoucePath = servletpath +
	 * pathinfo
	 * 
	 * @param requestURI
	 * @return resourcePath
	 */
	public static String getServletPath(String pattern, String resourcePath) {
		if (resourcePath == null)
			return null;
		int type = matchPattern(pattern, resourcePath);
		switch (type) {
		case PATTERN_DEFAULT_MATCH:
			return "";
		case PATTERN_EXT_MATCH:
		case PATTERN_EXACT_MATCH:
			return resourcePath;
		case PATTERN_PATH_MATCH:
			return pattern.substring(0, pattern.length() - "/*".length());
		default:
			throw new RuntimeException("PATTERN_NOMATCH pattern=" + pattern
					+ " path=" + resourcePath);
		}
	}

	/**
	 * リクエストURI中のコンテキストパス以外の部分を返します. resoucePath = servletpath + pathinfo
	 * 
	 * @param requestURI
	 * @return resourcePath
	 */
	public static String getPathInfo(String pattern, String resourcePath) {
		if (resourcePath == null)
			return null;
		int type = matchPattern(pattern, resourcePath);
		switch (type) {
		case PATTERN_DEFAULT_MATCH:
			return resourcePath;
		case PATTERN_EXT_MATCH:
		case PATTERN_EXACT_MATCH:
			return null;
		case PATTERN_PATH_MATCH:
			return resourcePath.substring(pattern.length() - "/*".length());
		default:
			throw new RuntimeException("PATTERN_NOMATCH pattern=" + pattern
					+ " path=" + resourcePath);
		}
	}

	/**
	 * パターンマッチなし
	 */
	public static final int PATTERN_NOMATCH = 0;
	/**
	 * デフォルトパターン(/*)でマッチ
	 */
	public static final int PATTERN_DEFAULT_MATCH = 1;
	/**
	 * 拡張子でマッチ
	 */
	public static final int PATTERN_EXT_MATCH = 2;
	/**
	 * パスでマッチ
	 */
	public static final int PATTERN_PATH_MATCH = 3;
	/**
	 * 完全マッチ
	 */
	public static final int PATTERN_EXACT_MATCH = 4;

	/**
	 * パスパターンと パスのマッチングを行います。
	 * 
	 * @param pattern
	 * @param path
	 * @return PATTERN_NOMATCH PATTERN_DEFAULT_MATCH PATTERN_EXT_MATCH
	 *         PATTERN_PATH_MATCH PATTERN_EXACT_MATCH
	 */
	public static int matchPattern(String pattern, String path) {
		if (pattern == null || path == null)
			return PATTERN_NOMATCH;

		if (pattern.equals(path))
			return PATTERN_EXACT_MATCH;

		if (pattern.equals("/*"))
			return PATTERN_DEFAULT_MATCH;

		if (pattern.endsWith("/*")) {
			if (pattern.regionMatches(0, path, 0, pattern.length() - 2)) {
				if (path.length() == (pattern.length() - 2))
					return PATTERN_PATH_MATCH;
				else if (path.charAt(pattern.length() - 2) == '/')
					return PATTERN_PATH_MATCH;
			}
			return PATTERN_NOMATCH;
		}

		if (pattern.startsWith("*.")) {
			int resourceDot = path.lastIndexOf(".");
			if (resourceDot >= 0) {
				String resourceExt = path.substring(resourceDot, path.length());
				int patternDot = pattern.indexOf(".");
				String patternExt = pattern.substring(patternDot, pattern
						.length());
				if (resourceExt.equals(patternExt))
					return PATTERN_EXT_MATCH;
			}
		}
		return PATTERN_NOMATCH;
	}

	/**
	 * Query部分を取り除きます
	 * 
	 * @param requestURI
	 * @return
	 */
	public static String stripQueryPart(String requestURI) {
		int queryIndex = requestURI.indexOf("?");
		if (queryIndex > 0) {
			return requestURI.substring(0, queryIndex);
		} else {
			return requestURI;
		}
	}

	/**
	 * Queary部分を取得します。ない場合、nullを返します。
	 * 
	 * @param requestURI
	 * @return
	 */
	public static String getQueryPart(String requestURI) {
		int queryIndex = requestURI.indexOf("?");
		if (queryIndex > 0) {
			return requestURI.substring(queryIndex + 1, requestURI.length());
		} else {
			return null;
		}
	}

	/**
	 * RequestURLを構築します。
	 * 
	 * @param scheme
	 * @param host
	 * @param port
	 * @param requestURI
	 * @return
	 */
	public static StringBuffer buildRequestURL(String scheme, String host,
			int port, String requestURI) {
		String portString;
		if (port == 80 && scheme.equals("http")) {
			portString = "";
		} else if (port == 443 && scheme.equals("https")) {
			portString = "";
		} else if (host.indexOf(":") != -1) {
			portString = "";
		} else {
			portString = ":" + port;
		}
		requestURI = stripQueryPart(requestURI);
		return new StringBuffer(scheme + "://" + host + portString + requestURI);
	}

	/**
	 * RequestURLを構築します。 host部分にポート指定が無い場合、schemeからポートを 設定します。
	 * 
	 * @param scheme
	 * @param host
	 * @param port
	 * @param requestURI
	 * @return
	 */
	public static StringBuffer buildRequestURL(String scheme, String host,
			String requestURI) {
		int portSep = host.indexOf(":");
		String portString;
		if (portSep != -1) {
			portString = host.substring(portSep + 1);
			host = host.substring(0, portSep);
		} else {
			if (scheme.equals("http"))
				portString = "";
			else if (scheme.equals("https"))
				portString = "";
			else
				throw new RuntimeException("schema:" + scheme + " not support.");
		}
		return buildRequestURL(scheme, host, Integer.parseInt(portString),
				requestURI);
	}

	public static void writeNotFoundPage(HttpServletResponse res)
			throws IOException {
		res.setStatus(HttpServletResponse.SC_NOT_FOUND);
		res.setContentType("text/html;charset=UTF-8");
		PrintWriter writer = res.getWriter();
		writer.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
		writer.println("<html><head>");
		writer.println("<title>404 Not Found</title>");
		writer.println("<head><body>");
		writer.println("<h1>Not Found</h1>");
		writer
				.println("<p>The requested URL resource was not found on this SDLoader.</p>");
		writer.println("<body></html>");
		writer.flush();
	}

	public static void writeInternalServerErrorPage(HttpServletRequest req,
			HttpServletResponse res, Throwable t) throws IOException {
		res.reset();
		res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		res.setContentType("text/html;charset=UTF-8");
		PrintWriter writer = res.getWriter();
		writer.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
		writer.println("<html><head>");
		writer.println("<title>500 Internal Server Error</title>");
		writer.println(getFoldingJS());
		writer.println("<head><body>");
		writer.println("<h1>500 Internal Server Error</h1>");
		writer.println(getFoldingContents("RequestHeader",
				getRequestHeaderContents(req), false));
		writer.println(getFoldingContents("RequestParameter",
				getRequestParameterContents(req), false));
		writer.println(getFoldingContents("RequestAttribute",
				getRequestAttrituteContents(req), false));
		writer.println(getFoldingContents("Session", getSessionContents(req),
				false));
		writer.println(getFoldingContents("Cookie", getCookieContents(req),
				false));
		if (t != null) {
			StringWriter exceptionWriter = new StringWriter();
			PrintWriter exceptionPrintWriter = new PrintWriter(exceptionWriter);
			exceptionPrintWriter.println("<pre style='color:#FF0000'>");
			t.printStackTrace(exceptionPrintWriter);
			exceptionPrintWriter.println("</pre>");
			exceptionPrintWriter.flush();
			writer.println(getFoldingContents("StackTrace", exceptionWriter
					.toString(), true));
		}
		writer.println("</body></html>");
		writer.flush();
	}

	private static String getRequestHeaderContents(HttpServletRequest req) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		Enumeration<String> names = req.getHeaderNames();
		if (!names.hasMoreElements()) {
			return "No Header.";
		}
		printWriter.write("<table width='100%'>");
		while (names.hasMoreElements()) {
			String key = names.nextElement();
			Enumeration<String> values = req.getHeaders(key);
			if (values != null) {
				while (values.hasMoreElements()) {
					printWriter
							.write("<tr style='background-color:#DDDDDD;'><td style='text-align:right;white-space:nowrap;'>"
									+ key
									+ "</td><td>"
									+ values.nextElement()
									+ "</td></tr>");
				}
			}
		}
		printWriter.write("</table>");
		printWriter.close();
		return stringWriter.toString();
	}

	private static String getRequestParameterContents(HttpServletRequest req) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		Enumeration<String> names = req.getParameterNames();
		if (!names.hasMoreElements()) {
			return "No Paramter.";
		}
		printWriter.write("<table width='100%'>");
		while (names.hasMoreElements()) {
			String key = names.nextElement();
			String[] values = req.getParameterValues(key);
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					printWriter
							.write("<tr style='background-color:#DDDDDD;'><td style='text-align:right;white-space:nowrap;'>"
									+ key
									+ "</td><td>"
									+ values[i]
									+ "</td></tr>");
				}
			}
		}
		printWriter.write("</table>");
		printWriter.close();
		return stringWriter.toString();
	}

	private static String getRequestAttrituteContents(HttpServletRequest req) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		Enumeration<String> names = req.getAttributeNames();
		if (!names.hasMoreElements()) {
			return "No Attribute.";
		}
		printWriter.write("<table width='100%'>");
		while (names.hasMoreElements()) {
			String key = names.nextElement();
			Object value = req.getAttribute(key);
			printWriter
					.write("<tr style='background-color:#DDDDDD;'><td style='text-align:right;white-space:nowrap;'>"
							+ key
							+ "</td><td>"
							+ value.toString()
							+ "</td></tr>");
		}
		printWriter.write("</table>");
		printWriter.close();
		return stringWriter.toString();
	}

	private static String getSessionContents(HttpServletRequest req) {
		HttpSession session = req.getSession(false);
		if (session == null) {
			return "No Session.";
		}
		Enumeration<String> names = session.getAttributeNames();
		if (!names.hasMoreElements()) {
			return "No Session.";
		}
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		printWriter.write("<table width='100%'>");
		while (names.hasMoreElements()) {
			String key = names.nextElement();
			Object value = session.getAttribute(key);
			printWriter
					.write("<tr style='background-color:#DDDDDD;'><td style='text-align:right;white-space:nowrap;'>"
							+ key
							+ "</td><td>"
							+ value.toString()
							+ "</td></tr>");
		}

		printWriter.write("</table>");
		printWriter.close();
		return stringWriter.toString();
	}

	private static String getCookieContents(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			return "No Cookie.";
		}
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		printWriter.write("<table width='100%'>");
		for (int i = 0; i < cookies.length; i++) {
			Cookie cookie = cookies[i];
			String valueText = "name=" + cookie.getName();
			valueText += " value=" + cookie.getValue();
			if (cookie.getPath() != null) {
				valueText += "　path=" + cookie.getPath();
			}
			if (cookie.getDomain() != null) {
				valueText += "　domain=" + cookie.getDomain();
			}
			if (cookie.getMaxAge() != 0) {
				valueText += "　maxAge=" + cookie.getMaxAge();
			}
			if (cookie.getComment() != null) {
				valueText += "　comment=" + cookie.getComment();
			}
			printWriter
					.write("<tr style='background-color:#DDDDDD;'><td style='white-space:nowrap;'>"
							+ valueText + "</td></tr>");
		}
		printWriter.write("</table>");
		printWriter.close();
		return stringWriter.toString();
	}

	private static String getFoldingContents(String name, String contents,
			boolean open) {
		String contentsStype = "style='display=" + (open ? "block" : "none")
				+ ";padding-left:6px'";
		return "<div id='title"
				+ name
				+ "' onclick='folding("
				+ "\"contents"
				+ name
				+ "\""
				+ ");return false;' style='padding-left:6px;padding-top:2px;paddin-bottom2px;margin:4px;color:white;background-color:black'>"
				+ name + "</div>" + "<div id='contents" + name + "' "
				+ contentsStype + ">" + contents + "</div>";
	}

	private static String getFoldingJS() {
		return "<script type='text/javascript'>\r\n" + "<!--\r\n"
				+ "function folding(targetId){\r\n"
				+ " var target = document.getElementById(targetId);\r\n"
				+ " if(target.style.display == 'none' ) {\r\n"
				+ "  target.style.display = 'block';\r\n" + " }else {\r\n"
				+ "  target.style.display = 'none';\r\n" + " }\r\n" + "}\r\n"
				+ "//-->\r\n" + "</script>\r\n";
	}
}
