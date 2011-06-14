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
package sdloader;

import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;

/**
 * SDLoaderをインメモリでオープンします.
 *
 * @deprecated
 * @author c9katayama
 */
@Deprecated
public class OpenWithInmemory {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(OpenWithInmemory.class);

	public static void main(String[] args) {
		try {
			SDLoader server = new SDLoader();
			server.getSDLoaderConfig().setConfig(
					SDLoader.KEY_SDLOADER_WAR_INMEMORY_EXTRACT, true);

			server.start();

			server.waitForStop();
		} catch (Throwable e) {
			log.error("SDLoader catch error.", e);
		}
		System.exit(0);
	}
}
