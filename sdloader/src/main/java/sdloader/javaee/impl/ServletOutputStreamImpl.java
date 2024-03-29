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

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import sdloader.http.HttpBody;

/**
 * ServletOutputStream実装クラス
 * 
 * @author c9katayama
 */
public class ServletOutputStreamImpl extends ServletOutputStream {
	private HttpBody body;

	private boolean close = false;

	public ServletOutputStreamImpl(HttpBody body) {
		this.body = body;
	}

	public void write(int b) throws IOException {
		body.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		body.write(b, off, len);
	}

	public void flush() throws IOException {
	}

	public void close() throws IOException {
		close = true;
	}

	// --no interface method
	public long getOutputSize() {
		return body.getSize();
	}

	public HttpBody getBody() {
		return body;
	}

	public boolean isClosed() {
		return close;
	}
}
