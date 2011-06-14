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

import java.util.Map;

import sdloader.util.CollectionsUtil;

/**
 * HTTPヘッダーの定数定義
 * 
 * @author c9katayama
 */
public class HttpConstants {

	public static final String HTTP_1_1 = "HTTP/1.1";

	public static final String SESSIONID_KEY = "JSESSIONID";

	public static final String CACHECONTROL = "Cache-Control";

	public static final String CONNECTION = "Connection";

	public static final String CONTENTLENGTH = "Content-Length";

	public static final String CONTENTRANGE = "Content-Range";

	public static final String CONTENTTYPE = "Content-Type";

	public static final String EXPIRES = "Expires";

	public static final String DATE = "Date";

	public static final String PRAGMA = "Pragma";

	public static final String TRANSFERENCODING = "Transfer-Encoding";

	public static final String LASTMODIFIED = "Last-Modified";

	/**
	 * Request
	 */
	public static final String ACCEPT = "Accept";

	public static final String HOST = "Host";

	public static final String IFMODIFIEDSINCE = "If-Modified-Since";

	public static final String KEEPALIVE = "Keep-Alive";

	public static final String USERAGENT = "User-Agent";

	/**
	 * Response
	 */
	public static final String SERVER = "Server";

	public static final String COOKIE = "Cookie";

	public static final String SETCOOKIE = "Set-Cookie";

	public static final String LOCATION = "Location";

	/**
	 * Field
	 */
	public static final String CHUNKED = "chunked";

	public static final String CLOSE = "close";

	public static final String WWW_FORM_URLENCODE = "application/x-www-form-urlencoded";

	/**
	 * method
	 */
	public static final String POST = "POST";

	public static final String GET = "GET";

	public static final String HEAD = "HEAD";

	public static final String PUT = "PUT";

	public static final String DELETE = "DELETE";

	public static final String TRACE = "TRACE";

	public static final String OPTION = "OPTION";

	/** token */
	public static final String CRLF_STRING = "\r\n";

	public static final String COLON_STRING = ": ";

	public static final String SEMI_COLON_STRING = "; ";

	/**
	 * HTTPステータス
	 */
	public static final int SC_CONTINUE = 100;

	public static final int SC_SWITCHING_PROTOCOLS = 101;

	public static final int SC_OK = 200;

	public static final int SC_CREATED = 201;

	public static final int SC_ACCEPTED = 202;

	public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;

	public static final int SC_NO_CONTENT = 204;

	public static final int SC_RESET_CONTENT = 205;

	public static final int SC_PARTIAL_CONTENT = 206;

	public static final int SC_MULTIPLE_CHOICES = 300;

	public static final int SC_MOVED_PERMANENTLY = 301;

	public static final int SC_MOVED_TEMPORARILY = 302;

	// public static final int SC_FOUND = 302;

	public static final int SC_SEE_OTHER = 303;

	public static final int SC_NOT_MODIFIED = 304;

	public static final int SC_USE_PROXY = 305;

	public static final int SC_TEMPORARY_REDIRECT = 307;

	public static final int SC_BAD_REQUEST = 400;

	public static final int SC_UNAUTHORIZED = 401;

	public static final int SC_PAYMENT_REQUIRED = 402;

	public static final int SC_FORBIDDEN = 403;

	public static final int SC_NOT_FOUND = 404;

	public static final int SC_METHOD_NOT_ALLOWED = 405;

	public static final int SC_NOT_ACCEPTABLE = 406;

	public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;

	public static final int SC_REQUEST_TIMEOUT = 408;

	public static final int SC_CONFLICT = 409;

	public static final int SC_GONE = 410;

	public static final int SC_LENGTH_REQUIRED = 411;

	public static final int SC_PRECONDITION_FAILED = 412;

	public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;

	public static final int SC_REQUEST_URI_TOO_LONG = 414;

	public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;

	public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;

	public static final int SC_EXPECTATION_FAILED = 417;

	public static final int SC_INTERNAL_SERVER_ERROR = 500;

	public static final int SC_NOT_IMPLEMENTED = 501;

	public static final int SC_BAD_GATEWAY = 502;

	public static final int SC_SERVICE_UNAVAILABLE = 503;

	public static final int SC_GATEWAY_TIMEOUT = 504;

	public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;

	private static Map<Integer, String> statusMap;
	static {
		statusMap = CollectionsUtil.newHashMap(50);
		statusMap.put(new Integer(SC_CONTINUE), "Continue");
		statusMap.put(new Integer(SC_SWITCHING_PROTOCOLS),
				"Switching Protocols");
		statusMap.put(new Integer(SC_OK), "OK");
		statusMap.put(new Integer(SC_CREATED), "Created");
		statusMap.put(new Integer(SC_ACCEPTED), "Accepted");
		statusMap.put(new Integer(SC_NON_AUTHORITATIVE_INFORMATION),
				"Non-Authoritative Information");
		statusMap.put(new Integer(SC_NO_CONTENT), "No Content");
		statusMap.put(new Integer(SC_RESET_CONTENT), "Reset Content");
		statusMap.put(new Integer(SC_PARTIAL_CONTENT), "Partial Content");
		statusMap.put(new Integer(SC_MULTIPLE_CHOICES), "Multiple Choices");
		statusMap.put(new Integer(SC_MOVED_PERMANENTLY), "Moved Permanently");
		statusMap.put(new Integer(SC_MOVED_TEMPORARILY), "Moved Temporarily");
		// statusMap.put(new Integer(SC_FOUND), "Found");
		statusMap.put(new Integer(SC_SEE_OTHER), "See Other");
		statusMap.put(new Integer(SC_NOT_MODIFIED), "Not Modified");
		statusMap.put(new Integer(SC_USE_PROXY), "Use Proxy");
		statusMap.put(new Integer(SC_TEMPORARY_REDIRECT), "Temporary Redirect");
		statusMap.put(new Integer(SC_BAD_REQUEST), "Bad Request");
		statusMap.put(new Integer(SC_UNAUTHORIZED), "Unauthorized");
		statusMap.put(new Integer(SC_PAYMENT_REQUIRED), "Payment Required");
		statusMap.put(new Integer(SC_FORBIDDEN), "Forbidden");
		statusMap.put(new Integer(SC_NOT_FOUND), "Not Found");
		statusMap.put(new Integer(SC_METHOD_NOT_ALLOWED), "Method Not Allowed");
		statusMap.put(new Integer(SC_NOT_ACCEPTABLE), "Not Acceptable");
		statusMap.put(new Integer(SC_PROXY_AUTHENTICATION_REQUIRED),
				"Proxy Authentication Required");
		statusMap.put(new Integer(SC_REQUEST_TIMEOUT), "Request Time-out");
		statusMap.put(new Integer(SC_CONFLICT), "Conflict");
		statusMap.put(new Integer(SC_GONE), "Gone");
		statusMap.put(new Integer(SC_LENGTH_REQUIRED), "Length Required");
		statusMap.put(new Integer(SC_PRECONDITION_FAILED),
				"Precondition Failed");
		statusMap.put(new Integer(SC_REQUEST_ENTITY_TOO_LARGE),
				"Request Entity Too Large");
		statusMap.put(new Integer(SC_REQUEST_URI_TOO_LONG),
				"Request-URI Too Large");
		statusMap.put(new Integer(SC_UNSUPPORTED_MEDIA_TYPE),
				"Unsupported Media Type");
		statusMap.put(new Integer(SC_REQUESTED_RANGE_NOT_SATISFIABLE),
				"Requested Range Not Satisfiable");
		statusMap.put(new Integer(SC_EXPECTATION_FAILED), "Expectation Failed");
		statusMap.put(new Integer(SC_INTERNAL_SERVER_ERROR),
				"Internal Server Error");
		statusMap.put(new Integer(SC_NOT_IMPLEMENTED), "Not Implemented");
		statusMap.put(new Integer(SC_BAD_GATEWAY), "Bad Gateway");
		statusMap.put(new Integer(SC_SERVICE_UNAVAILABLE),
				"Service Unavailable");
		statusMap.put(new Integer(SC_GATEWAY_TIMEOUT), "Gateway Time-out");
		statusMap.put(new Integer(SC_HTTP_VERSION_NOT_SUPPORTED),
				"HTTP Version not supported");
	}

	public static String findStatus(int statusCode) {
		return (String) statusMap.get(new Integer(statusCode));
	}

	private HttpConstants() {
		super();
	}
}
