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
package sdloader.http;

import java.io.OutputStream;

import sdloader.SDLoader;
import sdloader.javaee.impl.HttpServletRequestImpl;
import sdloader.javaee.impl.HttpServletResponseImpl;

/**
 * リクエストスコープ
 * 
 * @author c9katayama
 */
public class ProcessScopeContext {

	private static ThreadLocal<ProcessScopeContext> threadLocal = new ThreadLocal<ProcessScopeContext>();
	private SDLoader sdLoader;
	private int requestCount;
	private OutputStream outputStream;
	private HttpRequest httpRequest;
	private HttpServletRequestImpl request;
	private HttpServletResponseImpl response;

	private ProcessScopeContext() {
	}

	public static ProcessScopeContext getContext() {
		ProcessScopeContext context = threadLocal.get();
		if (context == null) {
			init();
			context = threadLocal.get();
		}
		return context;
	}

	public static void init() {
		threadLocal.set(new ProcessScopeContext());
	}

	public static void destroy() {
		threadLocal.remove();
	}

	public void setSDLoader(SDLoader sdLoader) {
		this.sdLoader = sdLoader;
	}

	public void setRequestCount(int requestCount) {
		this.requestCount = requestCount;
	}

	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	public void setHttpRequest(HttpRequest httpRequest) {
		this.httpRequest = httpRequest;
	}

	public void setRequestResponse(HttpServletRequestImpl request,
			HttpServletResponseImpl response) {
		this.request = request;
		this.response = response;
	}

	public SDLoader getSDLoader() {
		return sdLoader;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public HttpRequest getHttpRequest() {
		return httpRequest;
	}

	public HttpServletRequestImpl getRequest() {
		return request;
	}

	public HttpServletResponseImpl getResponse() {
		return response;
	}
}
