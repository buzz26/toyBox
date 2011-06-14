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
import java.io.InputStream;

import sdloader.constants.LineSpeed;

/**
 * 帯域制限つきInputStream
 * 
 * @author c9katayama
 * 
 */
public class QoSInputStream extends InputStream {

	private static final int SEC_SLICE_NUM = 4;
	private static final int BIT_PER_BYTE = 8;
	private static final int SLEEP_MILLI_SEC = 1000 / SEC_SLICE_NUM;

	private InputStream in;
	private int bytePerSliceSec;

	private int readBytes;
	private long nextReadTime;

	public QoSInputStream(InputStream in, int bps) {
		this.in = in;
		if (bps <= LineSpeed.NO_LIMIT) {
			bytePerSliceSec = Integer.MAX_VALUE;
		} else {
			bytePerSliceSec = Math.max(100,
					(int) (bps / BIT_PER_BYTE / SEC_SLICE_NUM));
		}
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public int read() throws IOException {
		if (nextReadTime == 0) {
			nextReadTime = System.currentTimeMillis() + SLEEP_MILLI_SEC;
		}
		if (readBytes == 0) {
			long sleepTime = nextReadTime - System.currentTimeMillis();
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
				}
			}
		}
		int b = in.read();
		readBytes++;
		if (readBytes == bytePerSliceSec) {
			readBytes = 0;
			nextReadTime = System.currentTimeMillis() + SLEEP_MILLI_SEC;
		}
		return b;
	}

}
