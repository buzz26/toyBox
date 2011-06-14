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

import sdloader.internal.CommandLineHelper;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.Browser;

/**
 * SDLoaderをオープンします.
 *
 * @author c9katayama
 */
public class Open {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(Open.class);

	protected String[] args;

	public static void main(String[] args) {
		new Open().open(args);
	}

	public void open(String[] args) {
		this.args = args;

		CommandLineHelper helper = createCommandLineHelper();
		if (helper.hasHelpOption()) {
			helper.printUsage(log);
			System.exit(0);
		}

		SDLoader sdloader = createSDLoader();
		try {
			helper.applySDLoaderProperties(sdloader);
		} catch (Exception e) {
			log.error(e.getMessage());
			helper.printHelpOption(log);
			System.exit(0);
		}
		try {
			sdloader.start();

			if (helper.isOpenBrowser()) {
				int port = sdloader.getPort();
				String url = "http://localhost:" + port;
				Browser.open(url);
			}
			if (helper.isWaitForStop()) {
				sdloader.waitForStop();
				System.exit(0);
			}

		} catch (Exception e) {
			log.error("SDLoader catch error.", e);
			System.exit(0);
		}
	}

	protected CommandLineHelper createCommandLineHelper() {
		CommandLineHelper helper = new CommandLineHelper(args);
		return helper;
	}

	protected SDLoader createSDLoader() {
		SDLoader sdloader = new SDLoader();
		sdloader.setAutoPortDetect(true);
		return sdloader;
	}
}
