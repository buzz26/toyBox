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
package sdloader.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLSocket;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import sdloader.SDLoader;
import sdloader.constants.HttpConstants;
import sdloader.constants.JavaEEConstants;
import sdloader.constants.LineSpeed;
import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.ServletMapping;
import sdloader.javaee.impl.FilterChainImpl;
import sdloader.javaee.impl.HttpServletRequestImpl;
import sdloader.javaee.impl.HttpServletResponseImpl;
import sdloader.javaee.impl.ServletContextImpl;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.IOUtil;
import sdloader.util.QoSInputStream;
import sdloader.util.QoSOutputStream;
import sdloader.util.ResourceUtil;
import sdloader.util.ThreadUtil;
import sdloader.util.WebUtil;

/**
 * ソケット接続に対して、処理を行います. リクエスト解析＞サーブレット呼び出し＞レスポンスの順に 処理を行います。
 * 
 * @author c9katayama
 */
public class HttpProcessor extends Thread {
	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(HttpProcessor.class);

	private int socketTimeout = 60 * 1000;

	private int keepAliveTimeout = 15 * 1000;// Apache 15

	private int keppAliveMaxRequests = 100;// Apache 100

	private AtomicBoolean running = new AtomicBoolean();

	private Socket socket;

	private SDLoader sdLoader;

	private HttpProcessorPool httpProcessorPool;

	public HttpProcessor(String name, SDLoader sdloader, HttpProcessorPool pool) {
		super(name);
		setDaemon(true);
		this.sdLoader = sdloader;
		this.httpProcessorPool = pool;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public void setKeepAliveTimeout(int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}

	public void setKeppAliveMaxRequests(int keppAliveMaxRequests) {
		this.keppAliveMaxRequests = keppAliveMaxRequests;
	}

	public void process(Socket socket) {
		synchronized (this) {
			this.socket = socket;
			notify();
		}
	}

	public boolean isRunning() {
		return running.get();
	}

	void stopProcessor() {
		running.set(false);
		ThreadUtil.notify(this);
	}

	public void run() {
		running.set(true);
		while (isRunning()) {
			synchronized (this) {
				if (socket == null) {
					ThreadUtil.wait(this);
				}
			}
			if (isRunning()) {
				processSocket();
			}
		}
	}

	protected void processSocket() {
		InputStream is = null;
		OutputStream os = null;
		try {
			socket.setTcpNoDelay(true);
			is = createInputStream(socket.getInputStream());
			os = createOutputStream(socket.getOutputStream());
			int requestCount = 1;
			while (true) {
				ProcessScopeContext.init();
				try {
					int timeout = (requestCount == 1) ? socketTimeout
							: keepAliveTimeout;
					socket.setSoTimeout(timeout);
					ProcessScopeContext scopeContext = ProcessScopeContext
							.getContext();
					scopeContext.setSDLoader(sdLoader);
					scopeContext.setRequestCount(requestCount);
					scopeContext.setOutputStream(os);
					HttpRequest httpRequest = processReadRequest(is);
					scopeContext.setHttpRequest(httpRequest);
					processServlet(httpRequest);
					if (httpRequest.getHeader().isKeepAlive()) {
						requestCount++;
					} else {
						break;
					}
				} finally {
					ProcessScopeContext.destroy();
				}
			}
		} catch (SocketTimeoutException e) {
			log.debug("socket timeout.");
		} catch (SocketException e) {
			log.debug("socket close.");
		} catch (Throwable t) {
			if (isRunning()) {
				log.error(t.getMessage(), t);
			}
		} finally {
			IOUtil.closeNoException(is, os);
			IOUtil.closeSocketNoException(socket);
			is = null;
			os = null;
			socket = null;
			if (isRunning()) {
				httpProcessorPool.returnProcessor(this);
			}
		}
	}

	protected HttpRequest processReadRequest(InputStream is) throws IOException {
		try {
			HttpRequestReader reader = new HttpRequestReader(is);
			HttpRequest httpRequest = new HttpRequest(reader);
			log.debug("request read start.");
			httpRequest.readRequest();
			log.debug("request read success.");
			if (log.isDebugEnabled()) {
				log.debug("<REQUEST_HEADER>\n"
						+ httpRequest.getHeader().getRequestHeader());
			}
			return httpRequest;
		} finally {
			if (socket.isClosed()) {
				throw new SocketException();
			} else {
				socket.setSoTimeout(socketTimeout);
			}
		}
	}

	protected void processServlet(HttpRequest httpRequest) throws Throwable {
		String requestURI = httpRequest.getHeader().getRequestURI();
		InternalWebApplication webapp = sdLoader.getWebAppManager().findWebApp(
				requestURI);
		HttpServletRequestImpl request = createServletRequestImp(httpRequest,
				webapp);
		ServletContextImpl servletContextImpl = webapp.getServletContext();
		String contextPath = webapp.getContextPath();
		String resourcePath = WebUtil.getResourcePath(contextPath, requestURI);

		// contextpathだけのパターン (/testのようなパターン）の場合、contextpathに/をつけてリダイレクト
		if (requestURI.equals("/") == false && resourcePath == null) {
			processRedirectToContextRoot(servletContextImpl, request,
					requestURI);
			return;
		}

		ServletMapping mapping = webapp.findServletMapping(resourcePath);
		if (mapping == null) {
			process404NotFound(servletContextImpl, request);
			return;
		}

		Servlet servlet = webapp.findServlet(mapping.getServletName());
		request.setServletPath(WebUtil.getServletPath(mapping.getUrlPattern(),
				resourcePath));
		request.setPathInfo(WebUtil.getPathInfo(mapping.getUrlPattern(),
				resourcePath));
		HttpServletResponseImpl response = new HttpServletResponseImpl();

		// class loader
		final ClassLoader oldLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					webapp.getWebAppClassLoader());
			ProcessScopeContext.getContext().setRequestResponse(request,
					response);
			request.intoScope();
			// service
			try {
				String servletName = mapping.getServletName();
				List<Filter> filterList = webapp.findFilters(resourcePath,
						servletName, JavaEEConstants.DISPATCHER_TYPE_REQUEST);
				if (filterList.size() > 0) {
					Filter[] filters = (Filter[]) filterList
							.toArray(new Filter[] {});
					FilterChainImpl filterChain = new FilterChainImpl(filters,
							servlet);
					filterChain.doFilter(request, response);
				} else {
					servlet.service(request, response);
				}
			} catch (ServletException se) {
				log.error(se.getMessage(), se);
				WebUtil.writeInternalServerErrorPage(request, response, se);
			} catch (IOException ioe) {
				log.error(ioe.getMessage(), ioe);
				WebUtil.writeInternalServerErrorPage(request, response, ioe);
			}
			setDefaultResponseHeader(servletContextImpl, request, response);
			processRequestEnd(response);
		} finally {
			request.dispose();
			response.dispose();
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}

	protected void processRedirectToContextRoot(
			ServletContextImpl servletContextImpl,
			HttpServletRequestImpl request, String requestURI)
			throws IOException {
		HttpServletResponseImpl response = new HttpServletResponseImpl();
		response.setStatus(HttpConstants.SC_MOVED_TEMPORARILY);
		String resourcePath = requestURI + "/";
		String host = request.getHeader(HttpConstants.HOST);
		if (host == null) {
			host = request.getLocalName() + ":" + request.getLocalPort();
		}
		response.addHeader(HttpConstants.LOCATION, WebUtil.buildRequestURL(
				request.getScheme(), host, resourcePath).toString());
		setDefaultResponseHeader(servletContextImpl, request, response);
		processRequestEnd(response);
	}

	protected void process404NotFound(ServletContextImpl servletContextImpl,
			HttpServletRequestImpl request) throws IOException {
		HttpServletResponseImpl response = new HttpServletResponseImpl();
		WebUtil.writeNotFoundPage(response);
		setDefaultResponseHeader(servletContextImpl, request, response);
		processRequestEnd(response);
	}

	private HttpServletRequestImpl createServletRequestImp(
			HttpRequest httpRequest, InternalWebApplication webApp) {
		HttpServletRequestImpl request = new HttpServletRequestImpl(
				httpRequest, webApp, sdLoader.getSessionManager());

		request.setServerPort(sdLoader.getPort());
		request.setLocalPort(socket.getLocalPort());
		request.setLocalAddr(socket.getLocalAddress().getHostAddress());
		request.setLocalName(socket.getLocalAddress().getHostName());

		request.setRemotePort(socket.getPort());
		request.setRemoteAddr(socket.getInetAddress().getHostAddress());
		request.setRemoteHost(socket.getInetAddress().getHostName());

		request.setScheme("http");

		String uriEncoding = sdLoader.getSDLoaderConfig()
				.getConfigStringIgnoreExist(
						HttpRequest.KEY_REQUEST_URI_ENCODING);
		if (uriEncoding != null && uriEncoding.length() > 0) {
			request.setUriEncoding(uriEncoding);
		}
		if (socket instanceof SSLSocket) {
			request.setSecure(true);
		}
		return request;
	}

	protected void setDefaultResponseHeader(
			ServletContextImpl servletContextImpl,
			HttpServletRequestImpl request, HttpServletResponseImpl response)
			throws IOException {
		response.setHeader(HttpConstants.DATE, WebUtil
				.formatHeaderDate(Calendar.getInstance().getTime()));
		response.setHeader(HttpConstants.SERVER, sdLoader.getServerName());

		// session
		HttpSession session = request.getSession(false);
		if (session != null && servletContextImpl != null) {
			String sessionId = session.getId();
			Cookie sessionCookie = new Cookie(HttpConstants.SESSIONID_KEY,
					sessionId);
			sessionCookie.setPath(servletContextImpl.getContextPath());
			response.addCookie(sessionCookie);
		}
		int requestCount = ProcessScopeContext.getContext().getRequestCount();
		// Keep-Alive
		if (request.getHeader().isKeepAlive()
				&& requestCount < keppAliveMaxRequests) {
			response.addHeader(HttpConstants.KEEPALIVE, "timeout="
					+ (int) keepAliveTimeout / 1000 + ", max="
					+ keppAliveMaxRequests);
			response.addHeader(HttpConstants.CONNECTION,
					HttpConstants.KEEPALIVE);
		} else {
			response.addHeader(HttpConstants.CONNECTION, HttpConstants.CLOSE);
		}
		// Cache Control
		if (sdLoader.getSDLoaderConfig().getConfigBoolean(
				HttpResponse.KEY_RESPONSE_USE_NOCACHE_MODE)) {
			if (sdLoader.getSDLoaderConfig().getConfigBoolean(
					SDLoader.KEY_SDLOADER_SSL_ENABLE) == false) {
				response.setHeader("Pragma", "no-cache");
				response.setHeader("Cache-Control",
						"no-cache, no-store, must-revalidate");
				response.setDateHeader("Expires", 1L);
			}
		}

		// Content-Length
		// Chunked以外はセット
		HttpHeader resHeader = response.getResponseHeader();
		String transferEncoding = resHeader
				.getHeaderValue(HttpConstants.TRANSFERENCODING);
		if (transferEncoding == null
				|| !transferEncoding.equalsIgnoreCase(HttpConstants.CHUNKED)) {
			response.setHeader(HttpConstants.CONTENTLENGTH, String
					.valueOf(response.getBodySize()));
		}
	}

	protected void processRequestEnd(HttpServletResponseImpl response)
			throws IOException {
		HttpHeader resHeader = response.getResponseHeader();
		OutputStream os = ProcessScopeContext.getContext().getOutputStream();

		byte[] headerData = resHeader.buildResponseHeader().getBytes();
		os.write(headerData);
		if (log.isDebugEnabled()) {
			log.debug("<RESPONSE_HEADER>\n" + new String(headerData));
		}
		os.write(HttpConstants.CRLF_STRING.getBytes());// Separator
		os.flush();
		HttpBody bodyData = response.getBodyData();
		if (bodyData != null) {
			ResourceUtil.copyStream(bodyData.getInputStream(), os);
		}
		os.flush();
	}

	protected InputStream createInputStream(InputStream is) {
		int lineSpeed = sdLoader.getSDLoaderConfig().getConfigInteger(
				SDLoader.KEY_SDLOADER_LINE_SPEED);
		if (lineSpeed <= LineSpeed.NO_LIMIT) {
			return is;
		} else {
			return new QoSInputStream(is, lineSpeed);
		}
	}

	protected OutputStream createOutputStream(OutputStream out) {
		int lineSpeed = sdLoader.getSDLoaderConfig().getConfigInteger(
				SDLoader.KEY_SDLOADER_LINE_SPEED);
		if (lineSpeed <= LineSpeed.NO_LIMIT) {
			return out;
		} else {
			return new QoSOutputStream(out, lineSpeed);
		}
	}
}
