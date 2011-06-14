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
/**
 * 
 */
package sdloader.javaee.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * RequestDispatcher#include()時のResponseWrapper
 * 
 * @author AKatayama
 * 
 */
public class IncludeResponseWrapper extends HttpServletResponseWrapper {
	public IncludeResponseWrapper(HttpServletResponse res) {
		super(res);
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(super.getWriter()) {
			@Override
			public void close() {
			}
		};
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		final ServletOutputStream out = super.getOutputStream();
		return new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				out.write(b, off, len);
			}

			@Override
			public void close() throws IOException {
			}
		};
	}

	@Override
	public void sendRedirect(String location) throws IOException {
	}

	@Override
	public void setBufferSize(int size) {
	}

	@Override
	public void setCharacterEncoding(String charset) {
	}

	@Override
	public void setContentLength(int len) {
	}

	@Override
	public void setDateHeader(String name, long date) {
	}

	@Override
	public void setIntHeader(String name, int value) {
	}

	@Override
	public void setLocale(Locale loc) {
	}

	@Override
	public void setStatus(int sc) {
	}

	@Override
	public void setStatus(int sc, String sm) {
	}

	@Override
	public void sendError(int sc) throws IOException {
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
	}

	@Override
	public void setHeader(String name, String value) {
	}

	@Override
	public void setContentType(String type) {
	}

	@Override
	public void addHeader(String name, String value) {
	}

	@Override
	public void addDateHeader(String name, long date) {
	}

	@Override
	public void addIntHeader(String name, int value) {
	}

	@Override
	public void addCookie(Cookie cookie) {
	}
}