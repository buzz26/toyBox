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
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * ByteBuffer利用のDataBuffer
 * 
 * @author c9katayama
 */
public class ByteDataBuffer implements DataBuffer {

	private static final int DEFAULT_BLOCK_SIZE = 16 * 1024;
	private LinkedList<ByteBuffer> bufferList = new LinkedList<ByteBuffer>();
	private ByteBuffer buffer;
	private long size;
	private int blockSize;

	public ByteDataBuffer() {
		this(DEFAULT_BLOCK_SIZE);
	}

	public ByteDataBuffer(int blockSize) {
		this.blockSize = blockSize;
		nextBuffer();
	}

	public long getSize() {
		return size;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void dispose() {
		bufferList = null;
		buffer = null;
		size = 0;
	}

	public OutputStream getOutputStream() throws IOException {
		return new DataBuffer.DelegateOutputStream(this);
	}

	public InputStream getInputStream() {
		return new ByteDataBufferInputStream(this);
	}

	public void write(int data) throws IOException {
		checkBuffer();
		buffer.put((byte) data);
		size++;
	}

	public void write(byte[] buf, int offset, int length) throws IOException {
		checkBuffer();
		if (buffer.position() + length > buffer.limit()) {
			do {
				checkBuffer();
				int copyLength = buffer.limit() - buffer.position();
				if (length < copyLength) {
					copyLength = length;
				}
				buffer.put(buf, offset, copyLength);
				offset += copyLength;
				length -= copyLength;
				size += copyLength;
			} while (length > 0);
		} else {
			buffer.put(buf, offset, length);
			size += length;
		}
	}

	public byte[] toByteArray() {
		if (size > Integer.MAX_VALUE) {
			throw new RuntimeException("too large size. size=" + size);
		}
		ByteBuffer data = ByteBuffer.allocate((int) size);
		for (ByteBuffer buf : bufferList) {
			data.put(buf.array(), 0, buf.position());
		}
		return data.array();
	}

	private final void checkBuffer() {
		if (buffer.position() == buffer.limit()) {
			nextBuffer();
		}
	}

	private final void nextBuffer() {
		buffer = ByteBuffer.allocate(blockSize);
		bufferList.addLast(buffer);
	}

	private static class ByteDataBufferInputStream extends InputStream {

		private ByteDataBuffer target;
		private ByteBuffer buffer;
		private int listIndex = -1;
		private long totalReadSize;

		private ByteDataBufferInputStream(ByteDataBuffer target) {
			this.target = target;
			checkBuffer();
		}

		@Override
		public int read() throws IOException {
			if (target.getSize() <= totalReadSize) {
				return -1;
			}
			checkBuffer();
			totalReadSize++;
			return buffer.get() & 0xff;
		}

		@Override
		public int read(byte[] b, int off, int length) throws IOException {
			long size = target.getSize();
			if (size <= totalReadSize) {
				return -1;
			}
			length = (int) Math.min(size - totalReadSize, length);
			final int actualReadSize = length;
			if (buffer.position() + length > buffer.limit()) {
				do {
					checkBuffer();
					int readLength = buffer.limit() - buffer.position();
					if (length < readLength) {
						readLength = length;
					}
					buffer.get(b, off, readLength);
					off += readLength;
					length -= readLength;
					totalReadSize += readLength;
				} while (length > 0);
			} else {
				buffer.get(b, off, length);
				totalReadSize += length;
			}
			return actualReadSize;
		}

		private final void checkBuffer() {
			if (buffer == null || buffer.position() == buffer.limit()) {
				listIndex++;
				buffer = target.bufferList.get(listIndex).asReadOnlyBuffer();
				buffer.clear();
			}
		}
	}
}
