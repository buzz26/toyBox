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
package sdloader.javaee.webxml;

import java.util.Iterator;
import java.util.List;

import sdloader.util.CollectionsUtil;

/**
 * web-appタグ
 * 
 * @author c9katayama
 * @author shot
 */
public class WebAppTag implements WebXmlTagElement {

	private List<ContextParamTag> contextParam = CollectionsUtil.newArrayList();

	private List<FilterTag> filter = CollectionsUtil.newArrayList();

	private List<FilterMappingTag> filterMapping = CollectionsUtil
			.newArrayList();

	private List<ListenerTag> listener = CollectionsUtil.newArrayList();

	private List<ServletTag> servlet = CollectionsUtil.newArrayList();

	private List<ServletMappingTag> servletMapping = CollectionsUtil
			.newArrayList();

	private WelcomeFileListTag welcomeFileList;

	public WebAppTag() {
		super();
	}

	public WebAppTag addContextParam(ContextParamTag tag) {
		this.contextParam.add(tag);
		return this;
	}

	public List<ContextParamTag> getContextParam() {
		return contextParam;
	}

	public WebAppTag addFilter(FilterTag filter) {
		this.filter.add(filter);
		return this;
	}

	public WebAppTag addFilterMapping(FilterMappingTag mapping) {
		this.filterMapping.add(mapping);
		return this;
	}

	public List<FilterTag> getFilter() {
		return filter;
	}

	public List<FilterMappingTag> getFilterMapping() {
		return filterMapping;
	}

	public FilterTag findFilter(FilterTag filterName) {
		for (Iterator<FilterTag> itr = filter.iterator(); itr.hasNext();) {
			FilterTag filter = itr.next();
			if (filter.getFilterName().equals(filterName)) {
				return filter;
			}
		}
		return null;
	}

	public FilterMappingTag findFiterMapping(String filterName) {
		for (Iterator<FilterMappingTag> itr = filterMapping.iterator(); itr
				.hasNext();) {
			FilterMappingTag filterMapping = itr.next();
			if (filterMapping.getFilterName().equals(filterName)) {
				return filterMapping;
			}
		}
		return null;
	}

	public WebAppTag addListener(ListenerTag tag) {
		this.listener.add(tag);
		return this;
	}

	public List<ListenerTag> getListener() {
		return listener;
	}

	public WebAppTag addServlet(ServletTag servlet) {
		this.servlet.add(servlet);
		return this;
	}

	public WebAppTag addServletMapping(ServletMappingTag mapping) {
		this.servletMapping.add(mapping);
		return this;
	}

	public List<ServletTag> getServlet() {
		return servlet;
	}

	public List<ServletMappingTag> getServletMapping() {
		return servletMapping;
	}

	public ServletTag findServlet(String servletName) {
		for (ServletTag servletTag : servlet) {
			if (servletTag.getServletName().equals(servletName)) {
				return servletTag;
			}
		}
		return null;
	}

	public ServletMappingTag findServletMapping(String servletName) {
		for (ServletMappingTag servletMappingTag : servletMapping) {
			if (servletMappingTag.getServletName().equals(servletName)) {
				return servletMappingTag;
			}
		}
		return null;
	}

	public WebAppTag setWelcomeFileList(WelcomeFileListTag welcomeFileList) {
		this.welcomeFileList = welcomeFileList;
		return this;
	}

	public WelcomeFileListTag getWelcomeFileList() {
		return welcomeFileList;
	}

	public void accept(WebXmlVisitor visitor) {
		visitor.visit(this);
	}

}
