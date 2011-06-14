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
package sdloader.util.databuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * DataBuffer
 * 
 * @author c9katayama
 */
public interface DataBuffer {

	long getSize();

	void write(int data) throws IOException;

	void write(byte[] buf, int offset, int length) throws IOException;

	InputStream getInputStream() throws IOException;

	OutputStream getOutputStream() throws IOException;

	void dispose();
	
	public static class DelegateOutputStream extends OutputStream{
		private DataBuffer delegate;
		public DelegateOutputStream(DataBuffer buf){
			this.delegate = buf;
		}
		@Override
		public void write(int b) throws IOException {
			this.delegate.write(b);
		}
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			this.delegate.write(b,off,len);
		}
	}
}
