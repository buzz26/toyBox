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

import sdloader.util.StringLoader;

/**
 * 
 * @author shot
 * 
 */
public class WebXmlStrings {

	public static String XML_HEADER;

	public static String WEBAPP_START_TAG;

	public static String WEBAPP_XML_NLS;

	public static String WEBAPP_XML_NLS_XSI;

	public static String WEBAPP_XSI;

	public static String VERSION;

	public static String WEBAPP_END_TAG;

	public static String CONTEXT_PARAM_START_TAG;

	public static String PARAM_NAME_START_TAG;

	public static String PARAM_NAME_END_TAG;

	public static String PARAM_VALUE_START_TAG;

	public static String PARAM_VALUE_END_TAG;

	public static String CONTEXT_PARAM_END_TAG;

	public static String FILTER_START_TAG;

	public static String FILTER_NAME_START_TAG;

	public static String FILTER_NAME_END_TAG;

	public static String FILTER_CLASS_START_TAG;

	public static String FILTER_CLASS_END_TAG;

	public static String FILTER_END_TAG;

	public static String INIT_PARAM_START_TAG;

	public static String INIT_PARAM_END_TAG;

	public static String FILTER_MAPPING_START_TAG;

	public static String URL_PATTERN_START_TAG;

	public static String URL_PATTERN_END_TAG;

	public static String FILTER_MAPPING_END_TAG;

	public static String DISPATCHER_START_TAG;

	public static String DISPATCHER_END_TAG;

	public static String LISTENER_START_TAG;

	public static String LISTENER_END_TAG;

	public static String LISTENER_CLASS_START_TAG;

	public static String LISTENER_CLASS_END_TAG;

	public static String SERVLET_START_TAG;

	public static String SERVLET_END_TAG;

	public static String SERVLET_NAME_START_TAG;

	public static String SERVLET_NAME_END_TAG;

	public static String SERVLET_CLASS_START_TAG;

	public static String SERVLET_CLASS_END_TAG;

	public static String LOADONSTARTUP_START_TAG;

	public static String LOADONSTARTUP_END_TAG;

	public static String SERVLET_MAPPING_START_TAG;

	public static String SERVLET_MAPPING_END_TAG;

	public static String WELCOME_FILE_LIST_START_TAG;

	public static String WELCOME_FILE_LIST_END_TAG;

	public static String WELCOME_FILE_START_TAG;

	public static String WELCOME_FILE_END_TAG;

	static {
		StringLoader.load(WebXmlStrings.class);
	}

}
