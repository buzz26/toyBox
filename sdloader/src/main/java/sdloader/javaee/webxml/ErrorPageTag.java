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
 * error-pageタグ
 * 
 * @author c9katayama
 */
public class ErrorPageTag implements WebXmlTagElement {

	private String errorCode;

	private String exceptionType;

	private String location;

	public String getErrorCode() {
		return errorCode;
	}

	public ErrorPageTag setErrorCode(String errorCode) {
		this.errorCode = errorCode;
		return this;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public ErrorPageTag setExceptionType(String exceptionType) {
		this.exceptionType = exceptionType;
		return this;
	}

	public String getLocation() {
		return location;
	}

	public ErrorPageTag setLocation(String location) {
		this.location = location;
		return this;
	}

	public String toXml() {
		return null;
	}

	public void accept(WebXmlVisitor visitor) {
		visitor.visit(this);
	}
}
