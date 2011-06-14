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

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sdloader.constants.JavaEEConstants;
import sdloader.http.HttpRequestParameters;
import sdloader.http.ProcessScopeContext;
import sdloader.javaee.ServletMapping;
import sdloader.util.WebUtil;

/**
 * RequestDispatcherの実装クラス
 * 
 * @author c9katayama
 */

@SuppressWarnings("unchecked")
public class RequestDispatcherImpl implements RequestDispatcher {

	private ServletContext dispatchServletContext;
	private Servlet dispatchServlet;
	private List<Filter> forwardFilterList;
	private List<Filter> includeFilterList;
	private ServletMapping dispatchServletMapping;
	private String contextPath;
	/** dispatch先のURI(コンテキストパス、クエリー込みのURI） */
	private String requestURI;
	private String queryString;

	RequestDispatcherImpl(ServletMapping dispatchServletMapping,
			Servlet dispatchServlet, List<Filter> forwardFilterList,
			List<Filter> includeFilterList,
			ServletContext dispatchServletContext, String contextPath,
			String dispatchURI) {
		this.dispatchServletContext = dispatchServletContext;
		this.dispatchServletMapping = dispatchServletMapping;
		this.dispatchServlet = dispatchServlet;
		this.forwardFilterList = forwardFilterList;
		this.includeFilterList = includeFilterList;
		this.contextPath = contextPath;
		this.requestURI = WebUtil.stripQueryPart(dispatchURI);
		this.queryString = WebUtil.getQueryPart(dispatchURI);
	}

	public void forward(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		setForwardAttribute(httpRequest);

		String resourcePath = WebUtil.getResourcePath(contextPath, requestURI);
		String servletPath = WebUtil.getServletPath(dispatchServletMapping
				.getUrlPattern(), resourcePath);
		String pathInfo = WebUtil.getPathInfo(dispatchServletMapping
				.getUrlPattern(), resourcePath);

		ForwardRequestWrapper requestWrapper = new ForwardRequestWrapper(
				httpRequest);
		requestWrapper.setServletContext(dispatchServletContext);
		requestWrapper.setRequestURI(requestURI);
		requestWrapper.setServletPath(servletPath);
		requestWrapper.setPathInfo(pathInfo);
		requestWrapper.setContextPath(contextPath);

		ProcessScopeContext processScopeContext = ProcessScopeContext
				.getContext();
		HttpServletRequestImpl firstRequestImpl = processScopeContext
				.getRequest();

		HttpRequestParameters.ParameterContext context = new HttpRequestParameters.ParameterContext();
		context.addAll(httpRequest.getParameterMap());
		if (queryString != null) {
			context.parseRequestQuery(queryString, firstRequestImpl
					.getParameters().getQueryEncoding());
		}
		requestWrapper.setMargedParameterContext(context);
		response.resetBuffer();

		doService(dispatchServlet, forwardFilterList, requestWrapper, response);
	}

	protected void setForwardAttribute(HttpServletRequest request) {
		if (request.getAttribute(JavaEEConstants.JAVAX_FORWARD_SERVLET_PATH) == null) {
			// 初回はパスを保存
			request.setAttribute(JavaEEConstants.JAVAX_FORWARD_REQUEST_URI,
					request.getRequestURI());
			request.setAttribute(JavaEEConstants.JAVAX_FORWARD_CONTEXT_PATH,
					request.getContextPath());
			request.setAttribute(JavaEEConstants.JAVAX_FORWARD_SERVLET_PATH,
					request.getServletPath());
			request.setAttribute(JavaEEConstants.JAVAX_FORWARD_PATH_INFO,
					request.getPathInfo());
			request.setAttribute(JavaEEConstants.JAVAX_FORWARD_QUERY_STRING,
					request.getQueryString());
		}
	}

	public void include(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		IncludeRequestWrapper requestWrapper = new IncludeRequestWrapper(
				(HttpServletRequest) request);
		IncludeResponseWrapper responseWrapper = new IncludeResponseWrapper(
				(HttpServletResponse) response);

		setIncludeAttribute(requestWrapper);

		ProcessScopeContext processScopeContext = ProcessScopeContext
				.getContext();
		HttpServletRequestImpl firstRequestImpl = processScopeContext
				.getRequest();

		HttpRequestParameters.ParameterContext context = new HttpRequestParameters.ParameterContext();
		context.addAll(httpRequest.getParameterMap());
		if (queryString != null) {
			context.parseRequestQuery(queryString, firstRequestImpl
					.getParameters().getQueryEncoding());
		}
		requestWrapper.setMargedParameterContext(context);

		doService(dispatchServlet, includeFilterList, requestWrapper,
				responseWrapper);

		removeIncludeAttribute(requestWrapper);
	}

	protected void setIncludeAttribute(HttpServletRequest request) {
		String resourcePath = WebUtil.getResourcePath(contextPath, requestURI);
		String servletPath = WebUtil.getServletPath(dispatchServletMapping
				.getUrlPattern(), resourcePath);
		String pathInfo = WebUtil.getPathInfo(dispatchServletMapping
				.getUrlPattern(), resourcePath);
		request.setAttribute(JavaEEConstants.JAVAX_INCLUDE_REQUEST_URI,
				requestURI);
		request.setAttribute(JavaEEConstants.JAVAX_INCLUDE_CONTEXT_PATH,
				contextPath);
		request.setAttribute(JavaEEConstants.JAVAX_INCLUDE_SERVLET_PATH,
				servletPath);
		request.setAttribute(JavaEEConstants.JAVAX_INCLUDE_PATH_INFO, pathInfo);
		request.setAttribute(JavaEEConstants.JAVAX_INCLUDE_QUERY_STRING,
				queryString);
	}

	protected void removeIncludeAttribute(HttpServletRequest request) {
		request.removeAttribute(JavaEEConstants.JAVAX_INCLUDE_REQUEST_URI);
		request.removeAttribute(JavaEEConstants.JAVAX_INCLUDE_CONTEXT_PATH);
		request.removeAttribute(JavaEEConstants.JAVAX_INCLUDE_SERVLET_PATH);
		request.removeAttribute(JavaEEConstants.JAVAX_INCLUDE_PATH_INFO);
		request.removeAttribute(JavaEEConstants.JAVAX_INCLUDE_QUERY_STRING);
	}

	private void doService(Servlet servlet, List<Filter> filterList,
			ServletRequest request, ServletResponse response)
			throws IOException, ServletException {
		if (filterList.size() > 0) {
			Filter[] filters = (Filter[]) filterList.toArray(new Filter[] {});
			FilterChainImpl filterChain = new FilterChainImpl(filters, servlet);
			filterChain.doFilter(request, response);
		} else {
			servlet.service(request, response);
		}
	}
}
