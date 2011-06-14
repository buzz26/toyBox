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

import javax.servlet.Servlet;

import sdloader.util.CollectionsUtil;

/**
 * servletタグ
 * 
 * @author c9katayama
 * @author shot
 */
public class ServletTag implements WebXmlTagElement {

	private String servletName;

	private String displayName;

	private String servletClass;

	private List<InitParamTag> initParam = CollectionsUtil.newArrayList();

	private Integer loadOnStartup;

	public ServletTag() {
		super();
	}

	public String getDisplayName() {
		return displayName;
	}

	public ServletTag setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	public ServletTag setLoadOnStartup(Integer loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
		return this;
	}

	public String getServletClass() {
		return servletClass;
	}

	public ServletTag setServletClass(Class<? extends Servlet> servletClass) {
		return setServletClass(servletClass.getName());
	}

	public ServletTag setServletClass(String servletClass) {
		this.servletClass = servletClass;
		return this;
	}

	public String getServletName() {
		return servletName;
	}

	public ServletTag setServletName(String servletName) {
		this.servletName = servletName;
		return this;
	}

	public ServletTag addInitParam(InitParamTag initParam) {
		this.initParam.add(initParam);
		return this;
	}

	public String getInitParam(String paramName) {
		for (Iterator<InitParamTag> itr = initParam.iterator(); itr.hasNext();) {
			InitParamTag initParamTag = itr.next();
			if (initParamTag.getParamName().equals(paramName)) {
				return initParamTag.getParamValue();
			}
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
