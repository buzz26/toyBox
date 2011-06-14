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
package sdloader.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.LogManager;

import sdloader.util.ResourceUtil;

/**
 * ログファクトリークラス
 * 
 * @author c9katayama
 */
public class SDLoaderLogFactory {

	private static boolean useSystemLog;
	static {
		InputStream is = getLogConfig();
		if (is == null) {
			useSystemLog = true;
			System.out
					.println("[SDLoader] Log configuration not found.use SDLoaderLogSystemImpl.");
		} else {
			try {
				// check class load
				if (checkUseSystemLog(is) == true) {
					useSystemLog = true;
					System.out
							.println("[SDLoader] Log class load fail by system class loader.use SDLoaderLogSystemImpl.");
				} else {
					is = getLogConfig();
					LogManager.getLogManager().readConfiguration(is);
				}
			} catch (Exception ioe) {
				useSystemLog = true;
				ioe.printStackTrace();
				System.out
						.println("[SDLoader] Log configuration fail.use SDLoaderLogSystemImpl.");
			}
		}
	}

	private static InputStream getLogConfig() {
		String configPath = System.getProperty("java.util.logging.config.file");
		if (configPath == null || configPath.length() == 0) {
			InputStream is = ResourceUtil.getResourceAsStream(
					"sdloader-logging.properties", SDLoaderLogFactory.class);
			if (is == null) {
				is = ResourceUtil.getResourceAsStream("sdloader.properties",
						SDLoaderLogFactory.class);
			}
			return is;
		}
		return null;
	}

	// クラスローダーの関係でSDLoaderのログがロードできない場合、SDLoaderLogSystemImplを使うかどうか
	private static boolean checkUseSystemLog(InputStream is) throws IOException {
		Properties p = new Properties();
		p.load(is);
		String[] handlers = p.getProperty("handlers").split(",");
		for (int i = 0; i < handlers.length; i++) {
			String target = handlers[i].trim();
			try {
				ClassLoader.getSystemClassLoader().loadClass(target);
			} catch (ClassNotFoundException e) {
				if (!target.startsWith("sdloader.log")) {
					System.err.println("Class not found. class=" + target);
				}
				return true;
			}
		}
		return false;
	}

	private SDLoaderLogFactory() {
	}

	public static SDLoaderLog getLog(Class<?> c) {
		if (useSystemLog) {
			return new SDLoaderLogSystemImpl(c);
		} else {
			return new SDLoaderLogJDKLoggerImpl(c);
		}
	}
}
