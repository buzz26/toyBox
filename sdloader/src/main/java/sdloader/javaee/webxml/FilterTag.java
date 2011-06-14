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

import javax.servlet.Filter;

import sdloader.util.CollectionsUtil;

/**
 * filterタグ
 * 
 * @author c9katayama
 */
public class FilterTag implements WebXmlTagElement {

	private String filterName;

	private String filterClass;

	private List<InitParamTag> initParam = CollectionsUtil.newArrayList();

	public FilterTag() {
		super();
	}

	public String getFilterClass() {
		return filterClass;
	}

	public FilterTag setFilterClass(Class<? extends Filter> filterClass) {
		return setFilterClass(filterClass.getName());
	}

	public FilterTag setFilterClass(String filterClass) {
		this.filterClass = filterClass;
		return this;
	}

	public String getFilterName() {
		return filterName;
	}

	public FilterTag setFilterName(String filterName) {
		this.filterName = filterName;
		return this;
	}

	public FilterTag addInitParam(InitParamTag initParam) {
		this.initParam.add(initParam);
		return this;
	}

	public String getInitParam(String paramName) {
		for (Iterator<InitParamTag> itr = initParam.iterator(); itr.hasNext();) {
			InitParamTag initParamTag = itr.next();
			if (initParamTag.getParamName().equals(paramName))
				return initParamTag.getParamValue();
		}
		return null;
	}

	public List<InitParamTag> getInitParamList() {
		return initParam;
	}

	public void accept(WebXmlVisitor visitor) {
		visitor.visit(this);
	}
}
