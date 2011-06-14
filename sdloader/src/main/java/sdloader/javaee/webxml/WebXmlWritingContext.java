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
 * 
 * @author shot
 * 
 */
public class WebXmlWritingContext {

	protected StringBuilder builder = new StringBuilder();

	protected String SEP = System.getProperty("line.separator");

	protected int depth = 0;

	protected String separator = "\t";

	public WebXmlWritingContext() {
	}

	public WebXmlWritingContext(String separator) {
		this.separator = separator;
	}

	public void appendHeader(String tag) {
		builder.append(tag);
		builder.append(SEP);
	}

	public void appendStartTag(String start) {
		builder.append(getSpaces() + start);
		builder.append(SEP);
		depth++;
	}

	public void appendEndTag(String end) {
		depth--;
		builder.append(getSpaces() + end);
		builder.append(SEP);
	}

	protected String getSpaces() {
		if (separator == null || separator.equals("")) {
			return "";
		}
		String s = "";
		for (int i = 0; i < depth; i++) {
			s += this.separator;
		}
		return s;
	}

	public void appendTag(String startTag, String value, String endTag) {
		builder.append(getSpaces() + startTag);
		builder.append(value);
		builder.append(endTag);
		builder.append(SEP);
	}

	public String getXml() {
		return new String(builder);
	}

	public void clear() {
		builder = new StringBuilder();
		depth = 0;
	}

}
