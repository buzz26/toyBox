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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import sdloader.constants.HttpConstants;
import sdloader.http.HttpBody;
import sdloader.http.HttpHeader;
import sdloader.util.IOUtil;
import sdloader.util.WebUtil;

/**
 * HttpServletResponse実装クラス
 * 
 * @author c9katayama
 */
public class HttpServletResponseImpl implements HttpServletResponse {

	private Locale locale = Locale.getDefault();

	private String characterEncoding = "ISO-8859-1";// J2EE specification

	private int bufferSize = 8192;

	private HttpHeader header = new HttpHeader();

	private HttpBody body;

	private ServletOutputStreamImpl internalOutputStream;

	private ServletOutputStream outputStream;

	private PrintWriter writer;

	private boolean committed = false;

	public HttpServletResponseImpl() {
		super();
	}

	public void addCookie(Cookie cookie) {
		header.addCookie(cookie);
	}

	public boolean containsHeader(String name) {
		return header.getHeaderValue(name) != null;
	}

	public String getContentType() {
		return header.getHeaderValue(HttpConstants.CONTENTTYPE);
	}

	public void setContentLength(int length) {
		setHeader(HttpConstants.CONTENTLENGTH, String.valueOf(length));
	}

	public void setContentType(String type) {
		setHeader(HttpConstants.CONTENTTYPE, type);
		String encodeInContentType = WebUtil.parseCharsetFromContentType(type);
		if (encodeInContentType != null){
			characterEncoding = encodeInContentType;
		}
	}

	public void setStatus(int sc) {
		header.setStatusCode(sc);
		header.setStatus(HttpConstants.findStatus(sc));
	}

	public void setStatus(int sc, String msg) {
		header.setStatus(msg);
		header.setStatusCode(sc);
	}

	public String encodeURL(String url) {
		return url;
	}

	public String encodeRedirectURL(String url) {
		return url;
	}

	public String encodeUrl(String uri) {
		return uri;
	}

	public String encodeRedirectUrl(String url) {
		return url;
	}

	public void sendError(int error, String message) throws IOException {
		setStatus(error, message);
		commit();
	}

	public void sendError(int error) throws IOException {
		setStatus(error);
		commit();
	}

	public void sendRedirect(String path) throws IOException {
		header.addHeader(HttpConstants.LOCATION, path);
		setStatus(HttpConstants.SC_MOVED_TEMPORARILY);
		commit();
	}

	public void setDateHeader(String name, long time) {
		setHeader(name, WebUtil.formatHeaderDate(new Date(time)));
	}

	public void addDateHeader(String name, long time) {
		addHeader(name, WebUtil.formatHeaderDate(new Date(time)));
	}

	public void setHeader(String name, String value) {
		this.header.setHeader(name, value);
	}

	public void addHeader(String name, String value) {
		if (isSingleValueHeader(name)) {
			setHeader(name, value);
		} else {
			this.header.addHeader(name, value);
		}
	}

	public void setIntHeader(String name, int value) {
		setHeader(name, String.valueOf(value));
	}

	public void addIntHeader(String name, int value) {
		addHeader(name, String.valueOf(value));
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (writer != null) {
			throw new IllegalStateException("getOutputStream was called.");
		}
		if (internalOutputStream == null) {
			initBody();
		}
		outputStream = internalOutputStream;
		return outputStream;
	}

	public PrintWriter getWriter() throws IOException {
		if (outputStream != null) {
			throw new IllegalStateException("getOutputStream was called.");
		}
		WebUtil.checkSupportedEndcoding(characterEncoding);
		if (internalOutputStream == null) {
			initBody();
		}
		if (writer == null) {
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					internalOutputStream, characterEncoding)));
		}
		return writer;
	}

	public void setCharacterEncoding(String encoding) {
		this.characterEncoding = encoding;
	}

	public void setBufferSize(int size) {
		this.bufferSize = size;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void flushBuffer() throws IOException {
		IOUtil.flushNoException(writer, internalOutputStream);
		committed = true;
	}

	public void resetBuffer() {
		if (isCommitted()) {
			throw new IllegalStateException("committed.");
		}
		initBody();
	}

	public void reset() {
		if (isCommitted()) {
			throw new IllegalStateException("committed.");
		}
		header = new HttpHeader();
		resetBuffer();
	}

	public boolean isCommitted() {
		return committed;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return locale;
	}

	// /non interface method
	public long getBodySize() throws IOException {
		flushBuffer();
		if (body != null) {
			return body.getSize();
		} else {
			return 0;
		}
	}

	public HttpBody getBodyData() throws IOException {
		commit();
		return body;
	}

	protected void commit() {
		IOUtil.closeNoException(writer, internalOutputStream);
		committed = true;
	}

	public HttpHeader getResponseHeader() {
		return header;
	}

	protected void initBody() {
		dispose();
		body = new HttpBody();
		internalOutputStream = new ServletOutputStreamImpl(body);
	}
	
	protected boolean isSingleValueHeader(String headerName) {
		return headerName.equalsIgnoreCase("Keep-Alive")
				|| headerName.equalsIgnoreCase("Connection")
				|| headerName.equalsIgnoreCase("Content-Type")
				|| headerName.equalsIgnoreCase("Content-Length");
	}

	public void dispose() {
		if (body != null) {
			body.dispose();
		}
		body = null;
		internalOutputStream = null;
		outputStream = null;
		writer = null;
	}
}
