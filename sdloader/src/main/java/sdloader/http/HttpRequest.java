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

import java.io.IOException;

import sdloader.SDLoader;
import sdloader.constants.HttpConstants;

/**
 * HTTPリクエスト ヘッダーパートとボディーパートを生成します。
 * 
 * @author c9katayama
 */
public class HttpRequest {

	/**
	 * デフォルトリクエストエンコード指定用キー
	 */
	public static final String KEY_REQUEST_DEFAULT_ENCODE = SDLoader.CONFIG_KEY_PREFIX
			+ "request.defaultEncode";
	/**
	 * GETパラメータにBODYのエンコードを適用するかどうかを指定するキー
	 */
	public static final String KEY_REQUEST_USE_BODY_ENCODEING_FOR_URI = SDLoader.CONFIG_KEY_PREFIX
			+ "request.useBodyEncodingURI";

	/**
	 * URIをエンコードするエンコード
	 */
	public static final String KEY_REQUEST_URI_ENCODING = SDLoader.CONFIG_KEY_PREFIX
			+ "request.URIEncoding";

	private HttpRequestReader requestReader;

	private HttpHeader header;
	private HttpBody body;
	private HttpRequestParameters parameters;

	public HttpRequest(HttpRequestReader requestReader) {
		this.requestReader = requestReader;
	}

	public void readRequest() throws IOException {
		createHttpRequestHeader();
		createHttpRequestBody();
		parameters = new HttpRequestParameters(header, body);
	}

	public HttpRequestParameters getParameters() {
		return parameters;
	}

	public HttpHeader getHeader() {
		return header;
	}

	public HttpBody getBody() {
		return body;
	}
	
	public void dispose(){
		body.dispose();
	}

	private void createHttpRequestHeader() throws IOException {
		StringBuilder httpHeaderBuf = new StringBuilder();
		String line = null;

		// skip blank line
		while (true) {
			line = requestReader.readHeaderLine();
			if (line.length() != 0) {
				break;
			}
		}
		while (true) {
			if (line.length() == 0) {
				break;
			}
			httpHeaderBuf.append(line);
			httpHeaderBuf.append(HttpConstants.CRLF_STRING);

			line = requestReader.readHeaderLine();
		}
		header = new HttpHeader(new String(httpHeaderBuf));
	}

	private void createHttpRequestBody() throws IOException {
		String contentLengthHeader = header
				.getHeaderValue(HttpConstants.CONTENTLENGTH);
		long contentLength = 0;
		if (contentLengthHeader != null) {
			contentLength = Long.parseLong(contentLengthHeader);
		}
		body = new HttpBody();
		if (contentLength > 0) {
			byte[] buf = new byte[4068];
			int size = -1;
			long readSize = 0;
			while (readSize < contentLength) {
				size = requestReader.readBody(buf);
				if (size == -1) {
					throw new IOException(
							"Invalide content length.Content length="
									+ contentLength + " ,Read size=" + readSize);
				}
				body.write(buf, 0, size);
				readSize += size;
			}
		}
	}
}
