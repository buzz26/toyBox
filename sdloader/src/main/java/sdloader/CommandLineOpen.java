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
import sdloader.util.Browser;

/**
 * SDLoaderをオープンします.
 *
 * @author c9katayama
 */
public class CommandLineOpen {

	public static void main(String[] args) {
		CommandLineHelper helper = new CommandLineHelper(args);
		if (helper.hasHelpOption()) {
			helper.printUsage();
			System.exit(0);
		}
		SDLoader sdloader = new SDLoader();
		try {
			helper.applySDLoaderProperties(sdloader);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			helper.printHelpOption();
			System.exit(0);
		}
		try {
			sdloader.start();

			if (helper.isOpenBrowser()) {
				int port = sdloader.getPort();
				String url = "http://localhost:" + port;
				Browser.open(url);
			}

			sdloader.waitForStop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}
