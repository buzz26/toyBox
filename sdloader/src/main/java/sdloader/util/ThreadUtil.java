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

/**
 * ThreadUtils
 * 
 * @author c9katayama
 */
public class ThreadUtil {

	public static void sleep(int sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (Exception e) {
			// ignore
		}
	}

	public static void join(Thread t) {
		try {
			t.join();
		} catch (InterruptedException e) {
			// ignore
		}
	}

	public static void wait(Object o) {
		try {
			synchronized (o) {
				o.wait();
			}
		} catch (InterruptedException e) {
			// ignore
		}
	}

	public static void notify(Object o) {
		synchronized (o) {
			o.notify();
		}
	}
}
