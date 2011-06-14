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
package sdloader.javaee.webxml.impl;

import java.text.MessageFormat;

import sdloader.javaee.webxml.ContextParamTag;
import sdloader.javaee.webxml.ErrorPageTag;
import sdloader.javaee.webxml.FilterMappingTag;
import sdloader.javaee.webxml.FilterTag;
import sdloader.javaee.webxml.InitParamTag;
import sdloader.javaee.webxml.ListenerTag;
import sdloader.javaee.webxml.ServletMappingTag;
import sdloader.javaee.webxml.ServletTag;
import sdloader.javaee.webxml.WebAppTag;
import sdloader.javaee.webxml.WebXmlStrings;
import sdloader.javaee.webxml.WebXmlVisitor;
import sdloader.javaee.webxml.WebXmlWritingContext;
import sdloader.javaee.webxml.WelcomeFileListTag;

/**
 * 
 * @author shot
 * 
 */
public class WebXmlVisitorImpl implements WebXmlVisitor {

	protected WebXmlWritingContext context;

	public WebXmlVisitorImpl() {
		this(new WebXmlWritingContext());
	}

	public WebXmlVisitorImpl(WebXmlWritingContext context) {
		this.context = context;
	}

	public void visit(WebAppTag tag) {
		context.appendHeader(WebXmlStrings.XML_HEADER);
		String startTag = WebXmlStrings.WEBAPP_START_TAG;
		String webApptag = MessageFormat.format(startTag,
				WebXmlStrings.WEBAPP_XML_NLS, WebXmlStrings.WEBAPP_XML_NLS_XSI,
				WebXmlStrings.WEBAPP_XSI, WebXmlStrings.VERSION);
		context.appendStartTag(webApptag);
		{
			for (ContextParamTag contextParamTag : tag.getContextParam()) {
				contextParamTag.accept(this);
			}
		}
		{
			for (FilterTag filterTag : tag.getFilter()) {
				filterTag.accept(this);
			}
			for (FilterMappingTag filterMappingTag : tag.getFilterMapping()) {
				filterMappingTag.accept(this);
			}
		}
		{
			for (ListenerTag listenerTag : tag.getListener()) {
				listenerTag.accept(this);
			}
		}
		{
			for (ServletTag servletTag : tag.getServlet()) {
				servletTag.accept(this);
			}
			for (ServletMappingTag servletMappingTag : tag.getServletMapping()) {
				servletMappingTag.accept(this);
			}
		}
		{
			WelcomeFileListTag welcomeFileListTag = tag.getWelcomeFileList();
			if (welcomeFileListTag != null) {
				welcomeFileListTag.accept(this);
			}

		}
		context.appendEndTag(WebXmlStrings.WEBAPP_END_TAG);
	}

	public void visit(ContextParamTag contextParamTag) {
		context.appendStartTag(WebXmlStrings.CONTEXT_PARAM_START_TAG);
		{
			context.appendTag(WebXmlStrings.PARAM_NAME_START_TAG,
					contextParamTag.getParamName(),
					WebXmlStrings.PARAM_NAME_END_TAG);
		}
		{
			context.appendTag(WebXmlStrings.PARAM_VALUE_START_TAG,
					contextParamTag.getParamValue(),
					WebXmlStrings.PARAM_VALUE_END_TAG);
		}
		context.appendEndTag(WebXmlStrings.CONTEXT_PARAM_START_TAG);
	}

	public void visit(FilterTag filterTag) {
		context.appendStartTag(WebXmlStrings.FILTER_START_TAG);
		{
			context.appendTag(WebXmlStrings.FILTER_NAME_START_TAG, filterTag
					.getFilterName(), WebXmlStrings.FILTER_NAME_END_TAG);
		}
		{
			context.appendTag(WebXmlStrings.FILTER_CLASS_START_TAG, filterTag
					.getFilterClass(), WebXmlStrings.FILTER_CLASS_END_TAG);
		}
		{
			for (InitParamTag initParamTag : filterTag.getInitParamList()) {
				initParamTag.accept(this);
			}
		}
		context.appendEndTag(WebXmlStrings.FILTER_END_TAG);
	}

	public void visit(InitParamTag initParamTag) {
		context.appendStartTag(WebXmlStrings.INIT_PARAM_START_TAG);
		{
			context.appendTag(WebXmlStrings.PARAM_NAME_START_TAG, initParamTag
					.getParamName(), WebXmlStrings.PARAM_NAME_END_TAG);
		}
		{
			context.appendTag(WebXmlStrings.PARAM_VALUE_START_TAG, initParamTag
					.getParamValue(), WebXmlStrings.PARAM_VALUE_END_TAG);
		}
		context.appendEndTag(WebXmlStrings.INIT_PARAM_END_TAG);
	}

	public void visit(FilterMappingTag filterMappingTag) {
		context.appendStartTag(WebXmlStrings.FILTER_MAPPING_START_TAG);
		{
			context.appendTag(WebXmlStrings.FILTER_NAME_START_TAG,
					filterMappingTag.getFilterName(),
					WebXmlStrings.FILTER_NAME_END_TAG);
		}
		final String urlPattern = filterMappingTag.getUrlPattern();
		final String servletName = filterMappingTag.getServletName();
		if (urlPattern != null) {
			context.appendTag(WebXmlStrings.URL_PATTERN_START_TAG, urlPattern,
					WebXmlStrings.URL_PATTERN_END_TAG);
		} else if (servletName != null) {
			context.appendTag(WebXmlStrings.SERVLET_NAME_START_TAG,
					servletName, WebXmlStrings.SERVLET_NAME_END_TAG);
		}
		{
			for (String dispatcher : filterMappingTag.getDispatchers()) {
				context.appendTag(WebXmlStrings.DISPATCHER_START_TAG,
						dispatcher, WebXmlStrings.DISPATCHER_END_TAG);
			}
		}
		context.appendEndTag(WebXmlStrings.FILTER_MAPPING_END_TAG);
	}

	public void visit(ListenerTag listenerTag) {
		context.appendStartTag(WebXmlStrings.LISTENER_START_TAG);
		context.appendTag(WebXmlStrings.LISTENER_CLASS_START_TAG, listenerTag
				.getListenerClass(), WebXmlStrings.LISTENER_CLASS_END_TAG);
		context.appendEndTag(WebXmlStrings.LISTENER_END_TAG);
	}

	public void visit(ServletTag servletTag) {
		context.appendStartTag(WebXmlStrings.SERVLET_START_TAG);
		{
			context.appendTag(WebXmlStrings.SERVLET_NAME_START_TAG, servletTag
					.getServletName(), WebXmlStrings.SERVLET_NAME_END_TAG);
		}
		{
			context.appendTag(WebXmlStrings.SERVLET_CLASS_START_TAG, servletTag
					.getServletClass(), WebXmlStrings.SERVLET_CLASS_END_TAG);
		}
		{
			for (InitParamTag initParamTag : servletTag.getInitParamList()) {
				initParamTag.accept(this);
			}
		}
		{
			Integer loadOnStartup = servletTag.getLoadOnStartup();
			if (loadOnStartup != null) {
				context.appendTag(WebXmlStrings.LOADONSTARTUP_START_TAG,
						loadOnStartup.toString(),
						WebXmlStrings.LOADONSTARTUP_END_TAG);
			}
		}
		context.appendEndTag(WebXmlStrings.SERVLET_END_TAG);
	}

	public void visit(ServletMappingTag servletMappingTag) {
		context.appendStartTag(WebXmlStrings.SERVLET_MAPPING_START_TAG);
		{
			context.appendTag(WebXmlStrings.SERVLET_NAME_START_TAG,
					servletMappingTag.getServletName(),
					WebXmlStrings.SERVLET_NAME_END_TAG);
		}
		{
			context.appendTag(WebXmlStrings.URL_PATTERN_START_TAG,
					servletMappingTag.getUrlPattern(),
					WebXmlStrings.URL_PATTERN_END_TAG);
		}
		context.appendEndTag(WebXmlStrings.SERVLET_MAPPING_END_TAG);
	}

	public void visit(WelcomeFileListTag welcomeFileListTag) {
		context.appendStartTag(WebXmlStrings.WELCOME_FILE_LIST_START_TAG);
		for (String welcomefile : welcomeFileListTag.getWelcomeFileList()) {
			context.appendTag(WebXmlStrings.WELCOME_FILE_START_TAG,
					welcomefile, WebXmlStrings.WELCOME_FILE_END_TAG);
		}
		context.appendEndTag(WebXmlStrings.WELCOME_FILE_LIST_END_TAG);
	}

	public void visit(ErrorPageTag errorPageTag) {
		// no op;
	}

	public WebXmlWritingContext getContext() {
		return context;
	}

}
