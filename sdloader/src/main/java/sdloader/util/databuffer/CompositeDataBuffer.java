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

public abstract class CompositeDataBuffer implements DataBuffer {

	protected DataBuffer firstDataBuffer;
	protected DataBuffer nextDataBuffer;

	protected boolean useNextBuffer = false;
	protected long size;
	protected long firstDataBufferLimitSize;

	public CompositeDataBuffer(long firstDataBufferLimitSize) {
		this.firstDataBufferLimitSize = firstDataBufferLimitSize;
		firstDataBuffer = createFirstDataBuffer();
	}

	public InputStream getInputStream() throws IOException {
		return new CompositeDataBufferInputStream(this);
	}

	public OutputStream getOutputStream() throws IOException {
		return new DataBuffer.DelegateOutputStream(this);
	}

	public long getSize() {
		return size;
	}

	public void write(byte[] buf, int offset, int length) throws IOException {
		if (useNextBuffer) {
			nextDataBuffer.write(buf, offset, length);
			size += length;
		} else {
			if ((size + length) > firstDataBufferLimitSize) {
				int writeSize = (int) (firstDataBufferLimitSize - size);
				firstDataBuffer.write(buf, offset, writeSize);
				size += writeSize;
				offset += writeSize;
				writeSize = length - writeSize;
				checkBuffer();
				write(buf, offset, writeSize);
			} else {
				firstDataBuffer.write(buf, offset, length);
				size += length;				
			}
		}		
	}

	public void write(int data) throws IOException {
		checkBuffer();
		if (useNextBuffer) {
			nextDataBuffer.write(data);
		} else {
			firstDataBuffer.write(data);
		}
		size++;
	}

	private void checkBuffer() {
		if (size == firstDataBufferLimitSize) {
			nextDataBuffer = createNextDataBuffer();
			useNextBuffer = true;
		}
	}

	public void dispose() {
		firstDataBuffer.dispose();
		if (nextDataBuffer != null) {
			nextDataBuffer.dispose();
		}
	}

	protected abstract DataBuffer createFirstDataBuffer();

	protected abstract DataBuffer createNextDataBuffer();

	private static class CompositeDataBufferInputStream extends InputStream {

		private InputStream firstDataBufferInputStream;
		private InputStream nextDataBufferInputStream;

		private CompositeDataBuffer target;
		private long totalReadSize;
		private boolean useNextBuffer = false;

		public CompositeDataBufferInputStream(CompositeDataBuffer target)
				throws IOException {
			this.target = target;
			firstDataBufferInputStream = target.firstDataBuffer
					.getInputStream();
		}

		@Override
		public int read() throws IOException {
			if (target.getSize() <= totalReadSize) {
				return -1;
			}
			checkStream();
			totalReadSize++;
			return useNextBuffer ? nextDataBufferInputStream.read()
					: firstDataBufferInputStream.read();
		}

		@Override
		public int read(byte[] buf, int offset, int length) throws IOException {
			if (target.getSize() <= totalReadSize) {
				return -1;
			}
			checkStream();
			int readSize = 0;
			if (useNextBuffer) {
				readSize = nextDataBufferInputStream.read(buf, offset, length);
			} else {
				if ((totalReadSize + length) > target.firstDataBufferLimitSize) {
					length = (int) (target.firstDataBufferLimitSize -totalReadSize);
				}
				readSize = firstDataBufferInputStream.read(buf, offset, length);
			}
			totalReadSize += readSize;
			return readSize;
		}

		private void checkStream() throws IOException {
			if (totalReadSize == target.firstDataBufferLimitSize) {
				useNextBuffer = true;
				nextDataBufferInputStream = target.nextDataBuffer
						.getInputStream();
			}
		}

		@Override
		public void close() throws IOException {
			firstDataBufferInputStream.close();
			if (nextDataBufferInputStream != null) {
				nextDataBufferInputStream.close();
			}
		}
	}
}
