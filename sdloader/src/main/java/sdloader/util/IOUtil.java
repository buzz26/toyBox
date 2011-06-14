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

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.Flushable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * 
 * @author c9katayama
 */
public class IOUtil {

	// 同一プロセス内で、すでに閉じたポート番号を記録
	private static Map<Integer, Boolean> closedPortMap;

	public static void clearClosedPortMap(){
		if(closedPortMap != null){
			closedPortMap.clear();
		}
	}
	public static boolean isClosedPort(int port) {
		if (closedPortMap != null) {
			Boolean open = closedPortMap.get(port);
			if (open != null && open == false) {
				return true;
			}
		}
		return false;
	}

	public static void setPortStatus(int port, boolean open) {
		if (closedPortMap == null) {
			closedPortMap = CollectionsUtil.newHashMap();
		}
		closedPortMap.put(port, open);
	}

	public static boolean forceRemoveDirectory(File directory) {
		if (!directory.exists()) {
			return true;
		}

		File[] files = directory.listFiles();
		if (files == null) {
			return directory.delete();
		}
		boolean result = true;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) {
				if (forceRemoveDirectory(file) == false) {
					result = false;
				}
			} else {
				if (file.delete() == false) {
					result = false;
				}
			}
		}
		if (directory.delete() == false) {
			result = false;
		}
		return result;
	}

	public static void flushNoException(Flushable... flushables) {
		if (flushables != null) {
			for (Flushable fl : flushables) {
				try {
					fl.flush();
				} catch (Exception ioe) {
					// ignore
				}
			}
		}
	}

	public static void closeNoException(Closeable... closeables) {
		if (closeables != null) {
			for (Closeable cl : closeables) {
				try {
					if (cl instanceof Flushable) {
						flushNoException((Flushable) cl);
					}
					cl.close();
				} catch (Exception ioe) {
					// ignore
				}
			}
		}
	}

	public static void closeHttpUrlConnectionNoException(
			HttpURLConnection... closeables) {
		if (closeables != null) {
			for (HttpURLConnection cl : closeables) {
				try {
					cl.disconnect();
				} catch (Exception ioe) {
					// ignore
				}
			}
		}
	}

	public static void closeServerSocketNoException(
			final ServerSocket serverSocket) {
		if (serverSocket != null) {
			try {
				int port = serverSocket.getLocalPort();
				serverSocket.close();
				IOUtil.setPortStatus(port, false);
			} catch (IOException e) {
			}
		}
	}

	public static void closeSocketNoException(final Socket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	public static ServerSocket createSSLServerSocket(int bindPort,
			boolean useOutSidePort, String keyStoreFilePath,
			String keyStorePassword, boolean reuse) throws IOException {
		try {
			KeyStore keyStore = KeyStore.getInstance("JKS");
			char[] password = keyStorePassword.toCharArray();
			keyStore.load(ResourceUtil.getResourceAsStream(keyStoreFilePath,
					IOUtil.class), password);
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance("SunX509");
			keyManagerFactory.init(keyStore, password);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
			ServerSocketFactory serverSocketFactory = sslContext
					.getServerSocketFactory();
			ServerSocket socket = serverSocketFactory.createServerSocket();
			socket.setReuseAddress(reuse);
			return bind(socket, bindPort, useOutSidePort);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static ServerSocket createServerSocket(int bindPort,
			boolean useOutSidePort, boolean reuseAddress) throws IOException {
		ServerSocket socket = new ServerSocket();
		socket.setReuseAddress(reuseAddress);
		return bind(socket, bindPort, useOutSidePort);
	}

	private static ServerSocket bind(ServerSocket socket, int bindPort,
			boolean useOutSidePort) throws IOException {
		try {
			if (useOutSidePort) {
				socket.bind(new InetSocketAddress(bindPort));
			} else {
				socket.bind(new InetSocketAddress(InetAddress
						.getByName("127.0.0.1"), bindPort));
			}
			IOUtil.setPortStatus(bindPort,true);
			return socket;
		} catch (IOException ioe) {
			closeServerSocketNoException(socket);
			throw ioe;
		}
	}

	public static FileFilter IGNORE_DIR_FILEFILTER = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory() && !file.getName().equals("CVS")
					&& !file.getName().startsWith(".");
		}
	};
	public static FileFilter WAR_FILEFILETR = new FileFilter() {
		public boolean accept(File file) {
			return file.getName().endsWith(".war");
		}
	};
	public static FileFilter XML_FILEFILTER = new FileFilter() {
		public boolean accept(File file) {
			return file.getName().endsWith(".xml");
		}
	};
	public static FileFilter JAR_ZIP_FILEFILTER = new FileFilter() {
		public boolean accept(File pathname) {
			if (pathname.getName().endsWith(".jar")
					|| pathname.getName().endsWith(".zip"))
				return true;
			else
				return false;
		}
	};
}
