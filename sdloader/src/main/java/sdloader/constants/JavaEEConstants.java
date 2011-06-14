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
package sdloader.constants;

import javax.servlet.ServletContext;

/**
 * JavaEEで定義されている定数.
 * 
 * @author c9katayama
 */
public class JavaEEConstants {

	public static final String JAVAX_INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";
	public static final String JAVAX_INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";
	public static final String JAVAX_INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";
	public static final String JAVAX_INCLUDE_PATH_INFO = "javax.servlet.include.path_info";
	public static final String JAVAX_INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

	public static final String JAVAX_FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";
	public static final String JAVAX_FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
	public static final String JAVAX_FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
	public static final String JAVAX_FORWARD_PATH_INFO = "javax.servlet.forward.path_info";
	public static final String JAVAX_FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";

	public static final String JAVAX_ERROR_STATUS_CODE = "javax.servlet.error.status_code";
	public static final String JAVAX_ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";
	public static final String JAVAX_ERROR_MESSAGE = "javax.servlet.error.message";
	public static final String JAVAX_ERROR_EXCEPTION = "javax.servlet.error.exception";
	public static final String JAVAX_ERROR_REQUEST_URI = "javax.servlet.error.request_uri";
	public static final String JAVAX_ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";

	public static final String DISPATCHER_TYPE_REQUEST = "REQUEST";
	public static final String DISPATCHER_TYPE_FORWARD = "FORWARD";
	public static final String DISPATCHER_TYPE_INCLUDE = "INCLUDE";
	public static final String DISPATCHER_TYPE_ERROR = "ERROR";

	public static Integer SERVLETAPI_MAJOR_VERSION = 2;
	public static Integer SERVLETAPI_MINOR_VERSION = 5;

	public static Integer JSP_MAJOR_VERSION = 2;
	public static Integer JSP_MINOR_VERSION = 1;

	static {
		try {
			SERVLETAPI_MAJOR_VERSION = 2;
			ServletContext.class.getMethod("getContextPath");
			SERVLETAPI_MINOR_VERSION = 5;
		} catch (Exception e) {
			SERVLETAPI_MINOR_VERSION = 4;
		}
		try {
			JSP_MAJOR_VERSION = 2;
			Class.forName("javax.servlet.jsp.el.ScopedAttributeELResolver");
			JSP_MINOR_VERSION = 1;
		} catch (Exception e) {
			try {
				Class.forName("javax.servlet.jsp.JspContext");
				JSP_MINOR_VERSION = 0;
			} catch (Exception ex) {
				JSP_MAJOR_VERSION = null;
				JSP_MINOR_VERSION = null;
			}
		}
	}
}
