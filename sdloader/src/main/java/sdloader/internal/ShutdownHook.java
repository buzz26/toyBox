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
package sdloader.internal;

import sdloader.SDLoader;

/**
 * Runtimeに引っ掛けるシャットダウンフック
 * 
 * @author c9katayama
 * 
 */
public class ShutdownHook extends Thread {

	private SDLoader sdLoader;

	public ShutdownHook(SDLoader loader) {
		this.sdLoader = loader;
		try {
			Runtime.getRuntime().addShutdownHook(this);
		} catch (Throwable t) {
			// ignore
		}
	}

	public void removeShutdownHook() {
		try {
			Runtime.getRuntime().removeShutdownHook(this);
		} catch (Throwable t) {
			// ignore
		}
	}

	@Override
	public void run() {
		try {
			sdLoader.stop();
		} catch (Throwable e) {
			// ignore
		}
	}
}
