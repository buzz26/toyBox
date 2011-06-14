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
package sdloader.util;

import java.io.IOException;
import java.io.OutputStream;

import sdloader.constants.LineSpeed;

/**
 * 帯域制限つきOutputStream
 * 
 * @author c9katayama
 * 
 */
public class QoSOutputStream extends OutputStream {

	private static final int SEC_SLICE_NUM = 4;
	private static final int BIT_PER_BYTE = 8;
	private static final int SLEEP_MILLI_SEC = 1000 / SEC_SLICE_NUM;

	private OutputStream out;
	private int bytePerSliceSec;

	private int writeBytes;
	private long nextWriteTime;

	public QoSOutputStream(OutputStream out, int bps) {
		this.out = out;
		if (bps <= LineSpeed.NO_LIMIT) {
			bytePerSliceSec = Integer.MAX_VALUE;
		} else {
			bytePerSliceSec = Math.max(100,
					(int) (bps / BIT_PER_BYTE / SEC_SLICE_NUM));
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (nextWriteTime == 0) {
			nextWriteTime = System.currentTimeMillis() + SLEEP_MILLI_SEC;
		}
		if (writeBytes == 0) {
			long sleepTime = nextWriteTime - System.currentTimeMillis();
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
			}
		}
		out.write(b);
		writeBytes++;
		if (writeBytes == bytePerSliceSec) {
			writeBytes = 0;
			nextWriteTime = System.currentTimeMillis() + SLEEP_MILLI_SEC;
		}
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
