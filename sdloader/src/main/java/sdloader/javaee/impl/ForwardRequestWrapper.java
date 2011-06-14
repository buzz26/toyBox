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

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import sdloader.http.HttpRequestParameters;
import sdloader.util.IteratorEnumeration;
import sdloader.util.WebUtil;

/**
 * RequestDispatcher#forward利用時のリクエストラッパー
 *
 * @author c9katayama
 */
public class ForwardRequestWrapper extends HttpServletRequestWrapper {

	private StringBuffer requestURL;
	private String requestURI;
	private String servletPath;
	private String pathInfo;
	private String contextPath;
	private ServletContext servletContext;

	private HttpRequestParameters.ParameterContext margedParameterContext;

	public ForwardRequestWrapper(HttpServletRequest req) {
		super(req);
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public StringBuffer getRequestURL() {
		if (requestURL == null) {
			requestURL = WebUtil.buildRequestURL(getScheme(), getLocalName(),
					getServerPort(), getRequestURI());
		}
		return requestURL;
	}

	public String getServletPath() {
		return servletPath;
	}

	public RequestDispatcher getRequestDispatcher(String requestURI) {
		return servletContext.getRequestDispatcher(requestURI);
	}

	public String getContextPath() {
		return contextPath;
	}

	@Override
	public String getParameter(String name) {
		return margedParameterContext.getParamter(name);
	}

	@Override
	public String[] getParameterValues(String name) {
		return margedParameterContext.getParameterValues(name);
	}

	@Override
	public Enumeration<?> getParameterNames() {
		return new IteratorEnumeration<String>(margedParameterContext
				.getParameterNames());
	}

	@Override
	public Map<?,?> getParameterMap() {
		return margedParameterContext.getParameterMap();
	}

	void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	void setMargedParameterContext(
			HttpRequestParameters.ParameterContext margedParameterContext) {
		this.margedParameterContext = margedParameterContext;
	}
}
