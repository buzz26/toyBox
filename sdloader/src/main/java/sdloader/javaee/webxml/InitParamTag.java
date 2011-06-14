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

/**
 * init-paramタグ
 * 
 * @author c9katayama
 * @author shot
 */
public class InitParamTag implements WebXmlTagElement {

	private String paramName;

	private String paramValue;

	public InitParamTag() {
	}

	public InitParamTag(String paramName, String paramValue) {
		setParam(paramName, paramValue);
	}

	public void setParam(String paramName, String paramValue) {
		setParamName(paramName);
		setParamValue(paramValue);
	}

	public String getParamName() {
		return paramName;
	}

	public InitParamTag setParamName(String paramName) {
		this.paramName = paramName;
		return this;
	}

	public String getParamValue() {
		return paramValue;
	}

	public InitParamTag setParamValue(String paramValue) {
		this.paramValue = paramValue;
		return this;
	}

	public void accept(WebXmlVisitor visitor) {
		visitor.visit(this);
	}
}
