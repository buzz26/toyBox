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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletException;

import sdloader.constants.JavaEEConstants;
import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.ListenerEventDispatcher;
import sdloader.javaee.ServletMapping;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.CollectionsUtil;
import sdloader.util.IteratorEnumeration;
import sdloader.util.Mime;
import sdloader.util.PathUtil;
import sdloader.util.ResourceUtil;

/**
 * ServletContext実装クラス
 * 
 * @author c9katayama
 */
public class ServletContextImpl implements ServletContext {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(ServletConfigImpl.class);

	private InternalWebApplication webApp;

	private String servletContextName;// コンテキスト名 /で始まるコンテキストディレクトリ名

	private URL[] docBase;// ドキュメントルート

	private Map<String, Servlet> servletMap;

	private Map<String, Object> attributeMap = CollectionsUtil.newHashMap();

	private Map<String, String> initParamMap = CollectionsUtil.newHashMap();

	public ServletContextImpl(InternalWebApplication webapp) {
		this.webApp = webapp;
	}

	public ServletContext getContext(String contextPath) {
		InternalWebApplication webapp = this.webApp.getWebApplicationManager()
				.findWebApp(contextPath);
		if (webapp != null) {
			return webapp.getServletContext();
		}

		return null;
	}

	public Set<String> getResourcePaths(String path) {
		if (path == null) {
			return null;
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		Set<String> pathSet = CollectionsUtil.newHashSet();
		for (URL base : docBase) {
			File baseFile = PathUtil.url2File(base);
			File targetResource = new File(PathUtil.jointPathWithSlash(baseFile
					.getAbsolutePath(), path));
			if (targetResource.exists()) {
				if (targetResource.isDirectory()) {
					File[] resources = targetResource.listFiles();
					if (resources != null) {
						for (int i = 0; i < resources.length; i++) {
							File resource = resources[i];
							String name = path + "/" + resource.getName();
							if (resource.isDirectory()) {
								name += "/";
							}
							pathSet.add(name);
						}
					}
				} else {
					String name = path + "/" + targetResource.getName();
					pathSet.add(name);
				}
			}
		}
		return pathSet.isEmpty() ? null : pathSet;
	}

	public URL getResource(String resource) throws MalformedURLException {
		URL url = null;
		if (ResourceUtil.isAbsoluteURL(resource)) {
			url = new URL(resource);
			return ResourceUtil.isResourceExist(url) ? url : null;
		} else {
			for (int i = 0; i < docBase.length; i++) {
				url = ResourceUtil.createURL(docBase[i], resource);
				if (ResourceUtil.isResourceExist(url)) {
					return url;
				}
			}
			return null;
		}
	}

	public InputStream getResourceAsStream(String resource) {
		try {
			URL url = getResource(resource);
			if (url != null) {
				return url.openStream();
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	public Servlet getServlet(String name) throws ServletException {
		if (servletMap == null) {
			return null;
		}
		return (Servlet) servletMap.get(name);
	}

	public Enumeration<Servlet> getServlets() {
		if (servletMap == null)
			return new IteratorEnumeration<Servlet>();
		return new IteratorEnumeration<Servlet>(servletMap.values().iterator());
	}

	public Enumeration<String> getServletNames() {
		if (servletMap == null) {
			return new IteratorEnumeration<String>();
		}
		return new IteratorEnumeration<String>(servletMap.keySet().iterator());
	}

	public String getRealPath(String resource) {
		if (resource == null) {
			return null;
		}
		if (docBase.length == 0) {
			URL url = ResourceUtil.createURL(docBase[0], resource);
			return toRealPath(url, resource);
		} else {
			for (int i = 0; i < docBase.length; i++) {
				URL url = ResourceUtil.createURL(docBase[i], resource);
				if (ResourceUtil.isResourceExist(url)) {
					return toRealPath(url, resource);
				}
			}
			URL url = ResourceUtil.createURL(docBase[0], resource);
			return toRealPath(url, resource);
		}
	}

	protected String toRealPath(URL url, String resource) {
		if (url.getProtocol().startsWith("file")) {
			try {
				File file = PathUtil.url2File(url);
				if (file.exists()) {
					String path = file.getAbsolutePath();
					// 指定のリソースがパスセパレータで終わっている場合は、返すパスもパスセパレータで終わる
					String testPath = PathUtil.replaceFileSeparator(path);
					String testResource = PathUtil
							.replaceFileSeparator(resource);
					if (testPath.endsWith("/") == false
							&& testResource.endsWith("/") == true) {
						path = path + File.separatorChar;
					}
					return path;
				} else {
					return null;
				}
			} catch (Exception e) {
				return null;
			}
		} else {
			return url.toExternalForm();
		}
	}

	public String getInitParameter(String key) {
		return (String) initParamMap.get(key);
	}

	public Enumeration<String> getInitParameterNames() {
		return new IteratorEnumeration<String>(initParamMap.keySet().iterator());
	}

	public Object getAttribute(String key) {
		return this.attributeMap.get(key);
	}

	public Enumeration<String> getAttributeNames() {
		return new IteratorEnumeration<String>(
				attributeMap.keySet().iterator(), true);
	}

	public void setAttribute(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException(
					"ServletContext attribute key is null.");
		}
		if (value == null) {
			removeAttribute(key);
		} else {
			Object oldValue = attributeMap.get(key);
			ListenerEventDispatcher dispatcher = webApp
					.getListenerEventDispatcher();
			if (oldValue == null) {
				this.attributeMap.put(key, value);
				ServletContextAttributeEvent event = new ServletContextAttributeEvent(
						this, key, value);
				dispatcher
						.dispatchServletContextAttributeListener_attributeAdded(event);
			} else if (oldValue != value) {
				this.attributeMap.put(key, value);
				ServletContextAttributeEvent event = new ServletContextAttributeEvent(
						this, key, oldValue);
				dispatcher
						.dispatchServletContextAttributeListener_attributeReplaced(event);
			}
		}
	}

	public void removeAttribute(String key) {
		Object value = this.attributeMap.remove(key);
		if (value != null) {
			ServletContextAttributeEvent event = new ServletContextAttributeEvent(
					this, key, value);
			ListenerEventDispatcher dispatcher = webApp
					.getListenerEventDispatcher();
			dispatcher
					.dispatchServletContextAttributeListener_attributeRemoved(event);
		}
	}

	public String getServletContextName() {
		return servletContextName;
	}

	public void log(String logValue) {
		log.info(logValue);
	}

	public void log(Exception ex, String logValue) {
		log.info(logValue, ex);
	}

	public void log(String logValue, Throwable t) {
		log.info(logValue, t);
	}

	public int getMajorVersion() {
		return JavaEEConstants.SERVLETAPI_MAJOR_VERSION;
	}

	public int getMinorVersion() {
		return JavaEEConstants.SERVLETAPI_MINOR_VERSION;
	}

	public String getMimeType(String path) {
		String ext = PathUtil.getExtension(path);
		if (ext == null) {
			return Mime.getMime(path.toLowerCase());
		} else {
			return Mime.getMime(ext.toLowerCase());
		}
	}

	/**
	 * パス名は "/" で始める必要があり、現在のコンテキストルートに対する相対パスとして解釈されます。
	 */
	public RequestDispatcher getRequestDispatcher(String requestPath) {
		if (!PathUtil.startsWithSlash(requestPath)) {
			throw new IllegalArgumentException(
					"dispatch path is not start with \"/\".");
		}
		InternalWebApplication webapp = webApp.getWebApplicationManager()
				.findWebApp(this.servletContextName);
		ServletMapping mapping = webapp.findServletMapping(requestPath);
		if (mapping == null) {
			return null;
		}
		String servletName = mapping.getServletName();
		Servlet servlet = webapp.findServlet(servletName);
		List<Filter> forwardFilters = webapp.findFilters(requestPath,
				servletName, JavaEEConstants.DISPATCHER_TYPE_FORWARD);
		List<Filter> includeFilters = webapp.findFilters(requestPath,
				servletName, JavaEEConstants.DISPATCHER_TYPE_INCLUDE);

		String contextPath = webapp.getContextPath();
		String requestURI = PathUtil.jointPathWithSlash(contextPath,
				requestPath);

		return new RequestDispatcherImpl(mapping, servlet, forwardFilters,
				includeFilters, webapp.getServletContext(), contextPath,
				requestURI);
	}

	public RequestDispatcher getNamedDispatcher(String servletName) {
		// throw new
		// NotImplementedYetException("ServletContext.getNamedDispatcher()");
		return null;
	}

	public String getServerInfo() {
		return "SDLoader";
	}

	public String getContextPath() {
		return servletContextName;
	}

	// /non interface method
	public void setServletMap(Map<String, Servlet> servletMap) {
		this.servletMap = servletMap;
	}

	public void addInitParameter(String paramName, String paramValue) {
		this.initParamMap.put(paramName, paramValue);
	}

	public void setServletContextName(String servletContextName) {
		this.servletContextName = servletContextName;
	}

	public void setDocBase(URL[] absoluteContextPath) {
		this.docBase = absoluteContextPath;
	}

	public InternalWebApplication getWebApplication() {
		return webApp;
	}
}
