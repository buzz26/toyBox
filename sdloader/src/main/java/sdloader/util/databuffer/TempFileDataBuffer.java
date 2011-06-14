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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import sdloader.exception.IORuntimeException;
import sdloader.util.IOUtil;

/**
 * TempFile利用のDataBuffer
 * 
 * @author c9katayama
 */
public class TempFileDataBuffer implements DataBuffer {

	private File tempFile;
	private OutputStream fileOutputStream;
	private List<InputStream> inputStreamList;
	private long size;
	private Thread removeThread = new Thread() {
		@Override
		public void run() {
			_dispose();
		}
	};

	public TempFileDataBuffer() {
		try {
			tempFile = File.createTempFile("sdl", ".tmp");
			tempFile.deleteOnExit();
			fileOutputStream = new FileOutputStream(tempFile);
			Runtime.getRuntime().addShutdownHook(removeThread);
		} catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	public long getSize() {
		return size;
	}

	public void dispose() {
		_dispose();
		if (removeThread != null) {
			try {
				Runtime.getRuntime().removeShutdownHook(removeThread);
			} catch (IllegalStateException e) {
				// ignore
			}
			removeThread = null;
		}
	}

	private void _dispose() {
		if (tempFile != null) {
			IOUtil.closeNoException(fileOutputStream);
			if (inputStreamList != null) {
				IOUtil.closeNoException(inputStreamList
						.toArray(new InputStream[] {}));
			}
			tempFile.delete();
			tempFile = null;
			fileOutputStream = null;
		}
	}

	public OutputStream getOutputStream() throws IOException {
		return new DataBuffer.DelegateOutputStream(this);
	}

	public InputStream getInputStream() throws IOException {
		if (inputStreamList == null) {
			inputStreamList = new ArrayList<InputStream>();
		}
		IOUtil.flushNoException(fileOutputStream);
		FileInputStream is = new FileInputStream(tempFile) {
			@Override
			public void close() throws IOException {
				super.close();
				inputStreamList.remove(this);
			}
		};
		inputStreamList.add(is);
		return is;
	}

	public void write(int data) throws IOException {
		fileOutputStream.write((byte) data);
		size++;
	}

	public void write(byte[] buf, int offset, int length) throws IOException {
		fileOutputStream.write(buf, offset, length);
		size += length;
	}
}
