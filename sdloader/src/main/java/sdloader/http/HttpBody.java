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
import java.io.InputStream;

import sdloader.util.databuffer.ByteDataBuffer;
import sdloader.util.databuffer.CompositeDataBuffer;
import sdloader.util.databuffer.DataBuffer;
import sdloader.util.databuffer.TempFileDataBuffer;

/**
 * HTTPのボディー部分
 * 
 * <pre>
 * バッファがBYTE_BUFFER_LIMIT以上になると、TempFileを使用するバッファに切り替えます。
 * </pre>
 * 
 * @author c9katayama
 */
public class HttpBody {

	public static final int BYTE_BUFFER_LIMIT = 4 * 1024 * 1024;
	private DataBuffer bodyData;

	public HttpBody() {
		bodyData = new CompositeDataBuffer(BYTE_BUFFER_LIMIT) {
			@Override
			protected DataBuffer createFirstDataBuffer() {
				return new ByteDataBuffer();
			}
			@Override
			protected DataBuffer createNextDataBuffer() {
				return new TempFileDataBuffer();
			}
		};
	}

	public long getSize() {
		return bodyData.getSize();
	}

	public void write(int data) throws IOException {
		bodyData.write(data);
	}

	public void write(byte[] buf, int offset, int length) throws IOException {
		bodyData.write(buf, offset, length);
	}

	public InputStream getInputStream() throws IOException {
		return bodyData.getInputStream();
	}

	public void dispose() {
		bodyData.dispose();
	}
}
