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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;

import sdloader.SDLoader;
import sdloader.constants.JavaEEConstants;
import sdloader.log.SDLoaderLog;
import sdloader.util.IOUtil;

/**
 * SDLoader処理用のHelperクラス
 * 
 * @author AKatayama
 * 
 */
public class SDLoaderHelper {

	private static final String SSL_KEY_STORE_PATH = "sdloader/resource/ssl/SDLoader.keystore";
	private static final String SSL_KEY_STORE_PASSWORD = "SDLoader";

	@SuppressWarnings("unused")
	private SDLoader sdLoader;

	public SDLoaderHelper(SDLoader loader) {
		this.sdLoader = loader;
	}

	public void printInitMessage(SDLoaderLog log) {
		String message = "Detect ServletAPI["
				+ JavaEEConstants.SERVLETAPI_MAJOR_VERSION + "."
				+ JavaEEConstants.SERVLETAPI_MINOR_VERSION + "]";
		if (JavaEEConstants.JSP_MAJOR_VERSION != null) {
			message += " JSP[" + JavaEEConstants.JSP_MAJOR_VERSION + "."
					+ JavaEEConstants.JSP_MINOR_VERSION + "]";
		} else {
			message += " JSP[NOT SUPPORT]";
		}
		log.info(message);
	}

	public ServerSocket createServerSocket(int bindPort, boolean sslEnable,
			boolean useOutSidePort, boolean reuse) throws IOException {
		if (sslEnable) {
			return IOUtil.createSSLServerSocket(bindPort, useOutSidePort,
					SSL_KEY_STORE_PATH, SSL_KEY_STORE_PASSWORD, reuse);
		} else {
			return IOUtil.createServerSocket(bindPort, useOutSidePort, reuse);
		}
	}

	public boolean tryConnectAndSendStopCommand(int port) {
		InputStream is = null;
		HttpURLConnection urlcon = null;
		try {
			URL stopUrl = new URL("http://127.0.0.1:" + port
					+ "/sdloader-command/stop");
			urlcon = (HttpURLConnection) stopUrl.openConnection();
			urlcon.setRequestMethod("POST");
			urlcon.setUseCaches(false);
			urlcon.setConnectTimeout(1000);
			urlcon.setReadTimeout(1000);
			urlcon.setDoInput(true);
			urlcon.setDoOutput(true);
			int responseCode = urlcon.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			return false;
		} finally {
			IOUtil.closeNoException(is);
			IOUtil.closeHttpUrlConnectionNoException(urlcon);
		}
	}
}
