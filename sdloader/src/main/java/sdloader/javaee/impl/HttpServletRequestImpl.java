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
package sdloader.javaee.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import sdloader.constants.HttpConstants;
import sdloader.http.HttpBody;
import sdloader.http.HttpHeader;
import sdloader.http.HttpRequest;
import sdloader.http.HttpRequestParameters;
import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.ListenerEventDispatcher;
import sdloader.javaee.SessionManager;
import sdloader.util.CollectionsUtil;
import sdloader.util.IteratorEnumeration;
import sdloader.util.PathUtil;
import sdloader.util.WebUtil;

/**
 * HttpServletRequest実装クラス
 * 
 * @author c9katayama
 * @author shot
 */
public class HttpServletRequestImpl implements HttpServletRequest {
	private StringBuffer requestURL;

	private String servletPath;

	private String pathInfo;

	private String protocol = "HTTP/1.1";

	private String scheme = "http";

	private int serverPort;

	private int localPort;

	private int remotePort;

	private String remoteAddr;

	private String localAddr;

	private String remoteHost;

	private String localName;

	private boolean secure;

	// 未セット時にはnullを返す為、HttpParemetersのbodyEncodingと2重持ち
	private String characterEncoding;

	private Locale locale = Locale.getDefault();

	private Map<String, Object> attributeMap = CollectionsUtil.newHashMap();

	private HttpRequest httpRequest;

	private SessionManager sessionManager;

	private InternalWebApplication internalWebApplication;

	private String uriEncoding = "ISO-8859-1";

	private String currentSessionId;

	public HttpServletRequestImpl(HttpRequest httpRequest,
			InternalWebApplication internalWebApplication,
			SessionManager sessionManager) {
		this.httpRequest = httpRequest;
		this.internalWebApplication = internalWebApplication;
		this.currentSessionId = getRequestedSessionId();
		this.sessionManager = sessionManager;
	}

	public String getHeader(String headerName) {
		return httpRequest.getHeader().getHeaderValue(headerName);
	}

	public Enumeration<String> getHeaders(String headerName) {
		return new IteratorEnumeration<String>(httpRequest.getHeader()
				.getHeaderValueList(headerName).iterator());
	}

	public Enumeration<String> getHeaderNames() {
		return new IteratorEnumeration<String>(httpRequest.getHeader()
				.getHeaderNameList().iterator());
	}

	public int getIntHeader(String paramName) {
		String val = httpRequest.getHeader().getHeaderValue(paramName);
		if (val != null) {
			return Integer.parseInt(val);
		}
		return -1;
	}

	public String getMethod() {
		return httpRequest.getHeader().getMethod();
	}

	public String getQueryString() {
		return httpRequest.getHeader().getQueryString();
	}

	public Object getAttribute(String key) {
		return attributeMap.get(key);
	}

	public Enumeration<String> getAttributeNames() {
		return new IteratorEnumeration<String>(
				attributeMap.keySet().iterator(), true);
	}

	/**
	 * 文字エンコードを返す。 setCharacterEncodingが呼ばれていない場合はnullを返す。
	 */
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String encoding)
			throws UnsupportedEncodingException {
		WebUtil.checkSupportedEndcoding(encoding);
		httpRequest.getParameters().setBodyEncoding(encoding);
		this.characterEncoding = encoding;
	}

	public int getContentLength() {
		String length = getHeader(HttpConstants.CONTENTLENGTH);
		if (length == null)
			return 0;
		else
			return Integer.parseInt(length);
	}

	public String getContentType() {
		return httpRequest.getHeader()
				.getHeaderValue(HttpConstants.CONTENTTYPE);
	}

	public ServletInputStream getInputStream() throws IOException {
		final InputStream is = httpRequest.getBody().getInputStream();
		ServletInputStream sIs = new ServletInputStream() {
			public int read() throws IOException {
				return is.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return is.read(b, off, len);
			}
		};
		return sIs;
	}

	public String getParameter(String key) {
		return httpRequest.getParameters().getParamter(key);
	}

	public Enumeration<String> getParameterNames() {
		Iterator<String> paramNameItr = httpRequest.getParameters()
				.getParameterNames();
		return new IteratorEnumeration<String>(paramNameItr);
	}

	public String[] getParameterValues(String key) {
		return httpRequest.getParameters().getParameterValues(key);
	}

	public Map<String, String[]> getParameterMap() {
		return httpRequest.getParameters().getParameterMap();
	}

	public String getProtocol() {
		return protocol;
	}

	public BufferedReader getReader() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				httpRequest.getBody().getInputStream()));
		return reader;
	}

	public void setAttribute(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException(
					"ServletRequest attribute key is null.");
		}
		if (value == null) {
			removeAttribute(key);
		} else {
			Object oldValue = attributeMap.get(key);
			ListenerEventDispatcher dispatcher = internalWebApplication
					.getListenerEventDispatcher();
			ServletContext sc = internalWebApplication.getServletContext();
			if (oldValue == null) {
				this.attributeMap.put(key, value);
				ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(
						sc, this, key, value);
				dispatcher
						.dispatchServletRequestAttributeListener_attributeAdded(event);
			} else if (oldValue != value) {
				this.attributeMap.put(key, value);
				ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(
						sc, this, key, oldValue);
				dispatcher
						.dispatchServletRequestAttributeListener_attributeReplaced(event);
			}
		}
	}

	public void removeAttribute(String key) {
		Object value = this.attributeMap.remove(key);
		if (value != null) {
			ServletContext sc = internalWebApplication.getServletContext();
			ServletRequestAttributeEvent event = new ServletRequestAttributeEvent(
					sc, this, key, value);
			ListenerEventDispatcher dispatcher = internalWebApplication
					.getListenerEventDispatcher();
			dispatcher
					.dispatchServletRequestAttributeListener_attributeRemoved(event);
		}
	}

	public String getContextPath() {
		return getServletContextImpl().getServletContextName();
	}

	public StringBuffer getRequestURL() {
		if (requestURL == null) {
			String host = httpRequest.getHeader().getHeaderValue(
					HttpConstants.HOST);
			requestURL = WebUtil.buildRequestURL(getScheme(), host,
					getServerPort(), getRequestURI());
		}
		return new StringBuffer(decodeURI(requestURL.toString()));
	}

	public Cookie[] getCookies() {
		List<Cookie> cookieList = httpRequest.getHeader().getCookieList();
		Cookie[] cookies = cookieList.toArray(new Cookie[] {});
		return cookies;
	}

	public HttpSession getSession() {
		return getSession(true);
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = sessionManager.getSession(currentSessionId,
				create, internalWebApplication);
		if (session != null) {
			currentSessionId = session.getId();
		}
		return session;
	}

	public String getRequestedSessionId() {
		Cookie cookie = httpRequest.getHeader().getCookie(
				HttpConstants.SESSIONID_KEY);
		return (cookie != null) ? cookie.getValue() : null;
	}

	public String getRequestURI() {
		return decodeURI(httpRequest.getHeader().getRequestURI());
	}

	public String getServletPath() {
		return decodeURI(servletPath);
	}

	public String getScheme() {
		return scheme;
	}

	public String getServerName() {
		String host = httpRequest.getHeader()
				.getHeaderValue(HttpConstants.HOST);
		if (host.indexOf(":") > 0)
			return host.substring(0, host.indexOf(":"));
		else
			return host;
	}

	public int getServerPort() {
		return serverPort;
	}

	public int getLocalPort() {
		return localPort;
	}

	public Locale getLocale() {
		return locale;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public String getLocalName() {
		return localName;
	}

	public Enumeration<Locale> getLocales() {
		Vector<Locale> vec = CollectionsUtil.newVector();
		Locale[] locales = Locale.getAvailableLocales();
		if (locales != null) {
			for (int i = 0; i < locales.length; i++) {
				vec.add(locales[i]);
			}
		}
		return vec.elements();
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		if (!PathUtil.startsWithSlash(path)) {
			String servletAndPathInfo = getServletPath();
			if (getPathInfo() != null) {
				servletAndPathInfo += getPathInfo();
			}
			path = PathUtil.computeRelativePath(servletAndPathInfo, path);
		}
		return getServletContextImpl().getRequestDispatcher(path);
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public String getPathInfo() {
		return decodeURI(pathInfo);
	}

	public boolean isSecure() {
		return secure;
	}

	public String getRealPath(String path) {
		return getServletContextImpl().getRealPath(path);
	}

	public String getLocalAddr() {
		return localAddr;
	}

	public String getAuthType() {
		return null;
	}

	public long getDateHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return -1;

		try {
			return WebUtil.parseHeaderDate(value);
		} catch (ParseException e) {
			throw new IllegalArgumentException("date format fail.header name="
					+ name + " value=" + value);
		}
	}

	public String getPathTranslated() {
		return null;
	}

	public String getRemoteUser() {
		return null;
	}

	public boolean isUserInRole(String arg0) {
		return false;
	}

	public Principal getUserPrincipal() {
		return null;
	}

	public boolean isRequestedSessionIdValid() {
		return true;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	// /non interfacemethod
	public void intoScope() {
		ServletContext sc = internalWebApplication.getServletContext();
		ServletRequestEvent event = new ServletRequestEvent(sc, this);
		ListenerEventDispatcher dispatcher = internalWebApplication
				.getListenerEventDispatcher();
		dispatcher.dispatchServletRequestListener_requestInitialized(event);
	}

	public void dispose() {
		httpRequest.dispose();
		if (internalWebApplication == null) {
			return;
		}
		ServletContext sc = internalWebApplication.getServletContext();
		ServletRequestEvent event = new ServletRequestEvent(sc, this);
		ListenerEventDispatcher dispatcher = internalWebApplication
				.getListenerEventDispatcher();
		dispatcher.dispatchServletRequestListener_requestDestroyed(event);
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public HttpHeader getHeader() {
		return this.httpRequest.getHeader();
	}

	public HttpBody getBody() {
		return this.httpRequest.getBody();
	}

	public HttpRequestParameters getParameters() {
		return this.httpRequest.getParameters();
	}

	public void setServerPort(int port) {
		this.serverPort = port;
	}

	public void setLocalPort(int port) {
		this.localPort = port;
	}

	public void setRemotePort(int port) {
		this.remotePort = port;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public void setUriEncoding(String uriEncoding) {
		if (uriEncoding != null) {
			this.uriEncoding = uriEncoding;
		}
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
		setScheme("https");
	}

	protected ServletContextImpl getServletContextImpl() {
		return internalWebApplication.getServletContext();
	}

	protected String decodeURI(String path) {
		if (path == null) {
			return null;
		}
		if ("ISO-8859-1".equals(uriEncoding)) {
			return path;
		}
		try {
			return URLDecoder.decode(path, uriEncoding);
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}