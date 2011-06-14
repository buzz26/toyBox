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

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import sdloader.util.CollectionsUtil;
import sdloader.util.IteratorEnumeration;

/**
 * FilterConfig実装クラス
 * 
 * @author c9katayama
 */
public class FilterConfigImpl implements FilterConfig {

	private Map<String, String> initParameter = CollectionsUtil.newHashMap();

	private String filterName;

	private ServletContext servletContext;

	public String getFilterName() {
		return filterName;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public String getInitParameter(String key) {
		return (String) initParameter.get(key);
	}

	public Enumeration<String> getInitParameterNames() {
		return new IteratorEnumeration<String>(initParameter.keySet()
				.iterator());
	}

	// non interface method
	public void addInitParameter(String key, String value) {
		initParameter.put(key, value);
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}
}
