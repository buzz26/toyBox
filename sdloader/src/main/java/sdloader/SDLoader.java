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

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import sdloader.constants.LineSpeed;
import sdloader.http.HttpProcessor;
import sdloader.http.HttpProcessorPool;
import sdloader.http.HttpRequest;
import sdloader.http.HttpResponse;
import sdloader.internal.SDLoaderConfig;
import sdloader.internal.SDLoaderHelper;
import sdloader.internal.ShutdownHook;
import sdloader.javaee.SessionManager;
import sdloader.javaee.WebAppContext;
import sdloader.javaee.WebAppManager;
import sdloader.lifecycle.Lifecycle;
import sdloader.lifecycle.LifecycleEvent;
import sdloader.lifecycle.LifecycleListener;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.ClassUtil;
import sdloader.util.IOUtil;
import sdloader.util.PathUtil;
import sdloader.util.ResourceUtil;
import sdloader.util.ThreadUtil;
import sdloader.util.event.EventDispatcher;

/**
 * SDLoader.
 * 
 * @author c9katayama
 */
public class SDLoader implements Lifecycle {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(SDLoader.class);

	public static final String CONFIG_KEY_PREFIX = "sdloader.";
	/**
	 * JSPコンパイル用のライブラリパス
	 * 
	 * <pre>
	 * SDLoader呼び出し前にセットすると、このパスのライブラリを利用します。
	 * </pre>
	 */
	public static final String KEY_SDLOADER_JSP_LIBPATH = CONFIG_KEY_PREFIX
			+ "jsp.libpath";
	/**
	 * SDLoaderのベースディレクトリパス
	 */
	public static final String KEY_SDLOADER_HOME = CONFIG_KEY_PREFIX + "home";

	public static final String KEY_SDLOADER_WEBAPPS_DIR = CONFIG_KEY_PREFIX
			+ "webAppsDir";
	/**
	 * @deprecated
	 */
	public static final String KEY_SDLOADER_WEBAPP_PATH = KEY_SDLOADER_WEBAPPS_DIR;

	public static final String KEY_SDLOADER_WAR_INMEMORY_EXTRACT = CONFIG_KEY_PREFIX
			+ "warInMemoryExtract";
	/**
	 * @deprecated
	 */
	public static final String KEY_WAR_INMEMORY_EXTRACT = KEY_SDLOADER_WAR_INMEMORY_EXTRACT;

	public static final String KEY_SDLOADER_USE_OUTSIDE_PORT = CONFIG_KEY_PREFIX
			+ "useOutSidePort";

	public static final String KEY_SDLOADER_MAX_THREAD_POOL_NUM = CONFIG_KEY_PREFIX
			+ "maxThreadPoolNum";

	public static final String KEY_SDLOADER_AUTO_PORT_DETECT = CONFIG_KEY_PREFIX
			+ "autoPortDetect";

	public static final String KEY_SDLOADER_SSL_ENABLE = CONFIG_KEY_PREFIX
			+ "sslEnable";

	public static final String KEY_SDLOADER_SERVER_NAME = CONFIG_KEY_PREFIX
			+ "serverName";

	public static final String KEY_SDLOADER_SESSION_MANAGER = CONFIG_KEY_PREFIX
			+ "sessionManager";

	public static final String KEY_SDLOADER_LINE_SPEED = CONFIG_KEY_PREFIX
			+ "lineSpeed";

	public static final String KEY_SDLOADER_PORT = CONFIG_KEY_PREFIX + "port";

	public static final String KEY_SDLOADER_WORK_DIR = CONFIG_KEY_PREFIX
			+ "workDir";

	public static final String KEY_SDLOADER_EXIT_ON_STOP = CONFIG_KEY_PREFIX
			+ "exitOnStop";

	public static final List<String> CONFIG_KEYS = new ArrayList<String>(14);
	static {
		CONFIG_KEYS.add(KEY_SDLOADER_JSP_LIBPATH);
		CONFIG_KEYS.add(KEY_SDLOADER_HOME);
		CONFIG_KEYS.add(KEY_SDLOADER_WEBAPPS_DIR);
		CONFIG_KEYS.add(KEY_SDLOADER_WEBAPP_PATH);
		CONFIG_KEYS.add(KEY_SDLOADER_WAR_INMEMORY_EXTRACT);
		CONFIG_KEYS.add(KEY_SDLOADER_USE_OUTSIDE_PORT);
		CONFIG_KEYS.add(KEY_SDLOADER_MAX_THREAD_POOL_NUM);
		CONFIG_KEYS.add(KEY_SDLOADER_AUTO_PORT_DETECT);
		CONFIG_KEYS.add(KEY_SDLOADER_SSL_ENABLE);
		CONFIG_KEYS.add(KEY_SDLOADER_SERVER_NAME);
		CONFIG_KEYS.add(KEY_SDLOADER_SESSION_MANAGER);
		CONFIG_KEYS.add(KEY_SDLOADER_LINE_SPEED);
		CONFIG_KEYS.add(KEY_SDLOADER_PORT);
		CONFIG_KEYS.add(KEY_SDLOADER_WORK_DIR);
		CONFIG_KEYS.add(KEY_SDLOADER_EXIT_ON_STOP);
	}

	private String sdloaderConfigPath = "sdloader.properties";

	protected WebAppManager webAppManager = new WebAppManager();

	protected SDLoaderConfig config = new SDLoaderConfig();

	protected EventDispatcher<LifecycleListener, LifecycleEvent<SDLoader>> dispatcher = new EventDispatcher<LifecycleListener, LifecycleEvent<SDLoader>>(
			LifecycleListener.class);

	public AtomicBoolean running = new AtomicBoolean();

	private SessionManager sessionManager;

	private SDLoaderThread sdLoaderThread;

	private ShutdownHook shutdownHook = new ShutdownHook(this);

	private SDLoaderHelper helper = new SDLoaderHelper(this);

	/**
	 * ポート30000でSDLoaderを構築します.
	 * 
	 * @param port
	 */
	public SDLoader() {
		loadDefaultConfig();
	}

	/**
	 * 指定のプロパティでSDLoaderを構築します.
	 * 
	 * @param port
	 */
	public SDLoader(Properties p) {
		loadDefaultConfig();
		config.addAll(p);
	}

	/**
	 * 指定のポートでSDLoaderを構築します.
	 * 
	 * @param port
	 */
	public SDLoader(int port) {
		loadDefaultConfig();
		setPort(port);
	}

	/**
	 * ポート30000でSDLoaderを構築します.
	 * 
	 * <pre>
	 * autoPortDetectがtrueの場合、ポート30000にバインド出来なかった時に
	 * 開きポートを探して起動します。
	 * </pre>
	 * 
	 * @param autoPortDetect
	 */
	public SDLoader(boolean autoPortDetect) {
		loadDefaultConfig();
		setAutoPortDetect(autoPortDetect);
	}

	public SDLoader(String defaultConfigPath) {
		sdloaderConfigPath = defaultConfigPath;
		loadDefaultConfig();
	}

	protected void loadDefaultConfig() {

//2011/06/14 kimukou.buzz add start
		EnvUtil.setENV();
		SDLoader.log.info("SDLoader[JENKINS_HOME:" + System.getenv("JENKINS_HOME") + "] ");
//2011/06/14 kimukou.buzz add end

		config.setConfig(KEY_SDLOADER_SESSION_MANAGER,
				"sdloader.javaee.impl.SessionManagerImpl");
		config.setConfig(KEY_SDLOADER_WAR_INMEMORY_EXTRACT, false);
		setSSLEnable(false);
		setUseOutSidePort(false);
		setMaxThreadPoolNum(2);
		setAutoPortDetect(false);
		setServerName("SDLoader");
		setPort(30000);
		setUseNoCacheMode(false);
		setLineSpeed(LineSpeed.NO_LIMIT);
		setExitOnStop(false);

		Properties initSetting = ResourceUtil.loadProperties(
				sdloaderConfigPath, SDLoader.class);
		if (initSetting != null) {
			config.addAll(initSetting);
		}
	}

	public void setConfig(String key, String value) {
		getSDLoaderConfig().setConfig(key, value);
	}

	public SDLoaderConfig getSDLoaderConfig() {
		return config;
	}

	/**
	 * ポートが使用中の場合、使用できるポートを探すかどうかをセットします.
	 * <p>
	 * trueの場合、セットしたポートが使用中の場合、別のポートを探します。
	 * </p>
	 * 
	 * @param key
	 * @return
	 */
	public void setAutoPortDetect(boolean autoPortDetect) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_AUTO_PORT_DETECT, autoPortDetect);
	}

	/**
	 * ポートを外部からの接続に応答するようにするかどうかをセットします。
	 * 
	 * @param useOutSizePort
	 */
	public void setUseOutSidePort(boolean useOutSizePort) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_USE_OUTSIDE_PORT, Boolean
				.toString(useOutSizePort));
	}

	/**
	 * 最大スレッドプール数を設定します。 open前にセットしてください。
	 * 
	 * @param maxThreadPoolNum
	 */
	public void setMaxThreadPoolNum(int maxThreadPoolNum) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_MAX_THREAD_POOL_NUM, maxThreadPoolNum);
	}

	/**
	 * URIのエンコードをセットします。
	 * 
	 * @param encoding
	 */
	public void setURIEncoding(String uriEncoding) {
		checkNotRunning();
		config.setConfig(HttpRequest.KEY_REQUEST_URI_ENCODING, uriEncoding);
	}

	/**
	 * trueの場合、Body部分のエンコードをGETパラメータにも適用します。
	 * 
	 * @param encoding
	 */
	public void setUseBodyEncodingURI(boolean useBodyEncodingForURI) {
		checkNotRunning();
		config.setConfig(HttpRequest.KEY_REQUEST_USE_BODY_ENCODEING_FOR_URI,
				useBodyEncodingForURI);
	}

	/**
	 * trueの場合、すべてのレスポンスにNoCacheヘッダーをつけます。
	 * 
	 * @param useNoCacheMode
	 */
	public void setUseNoCacheMode(boolean useNoCacheMode) {
		checkNotRunning();
		config.setConfig(HttpResponse.KEY_RESPONSE_USE_NOCACHE_MODE,
				useNoCacheMode);
	}

	/**
	 * Listenするポート番号を設定します。 open前にセットしてください。
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_PORT, port);
	}

	/**
	 * サーバ名を設定します。レスポンスのServerヘッダーに この名前が入ります。
	 * 
	 * @param serverName
	 */
	public void setServerName(String serverName) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_SERVER_NAME, serverName);
	}

	/**
	 * 回線速度をセットします。
	 * 
	 * @param bps
	 */
	public void setLineSpeed(int bps) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_LINE_SPEED, bps);
	}

	/**
	 * Webアプリの入っているディレクトリを指定します。
	 * 
	 * <pre>
	 * デフォルトは、SDLoader起動ディレクトリ下のwebappsフォルダです。
	 * このフォルダの下にディレクトリ、warファイル、コンテキストファイルを置くと、
	 * Webアプリとして認識されます。
	 * </pre>
	 * 
	 * @param dir
	 */
	public void setWebAppsDir(String dir) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_WEBAPPS_DIR, dir);
	}

	/**
	 * JSPコンパイルやwarの展開を行うディレクトリをセットします。
	 * 
	 * @param workDir
	 */
	public void setWorkDir(String workDir) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_WORK_DIR, workDir);
	}

	/**
	 * SSLを使用するかどうか.
	 * 
	 * <pre>
	 * trueの場合、SSLでソケットを開きます。
	 * デフォルトはfalseです。
	 * </pre>
	 * 
	 * @param value
	 */
	public void setSSLEnable(boolean value) {
		checkNotRunning();
		config.setConfig(KEY_SDLOADER_SSL_ENABLE, value);
	}

	public boolean isSSLEnable() {
		return config.getConfigBoolean(KEY_SDLOADER_SSL_ENABLE);
	}

	/**
	 * stopメソッドを呼んだ場合にSystem.exit()を呼ぶかどうか.
	 * 
	 * <pre>
	 * SDLoaderはメインスレッド（SDLoaderThread)以外はデーモンスレッドのため、
	 * stop()を呼び出すことでSDLoader関連のすべてのスレッドが終了するため、他にスレッドがなければ
	 * システムが停止します。
	 * SDLoader以外でスレッドを実行しており、SDLoader#stop()呼出し時にSystem.exit()を呼びたい場合に
	 * trueをセットします。
	 * デフォルトはfalseです。
	 * </pre>
	 * 
	 * @param value
	 */
	public void setExitOnStop(boolean value) {
		config.setConfig(KEY_SDLOADER_EXIT_ON_STOP, value);
	}

	public boolean isExitOnStop() {
		return config.getConfigBoolean(KEY_SDLOADER_EXIT_ON_STOP);
	}

	/**
	 * Webアプリケーションコンテキストを追加します。
	 * 
	 * @param context
	 */
	public void addWebAppContext(WebAppContext context) {
		checkNotRunning();
		webAppManager.addWebAppContext(context);
	}

	/**
	 * Listenするポート番号を返します。
	 * 
	 * @return
	 */
	public int getPort() {
		return config.getConfigInteger(KEY_SDLOADER_PORT);
	}

	/**
	 * サーバ名を返します。
	 * 
	 * @return
	 */
	public String getServerName() {
		return config.getConfigString(KEY_SDLOADER_SERVER_NAME);
	}

	/**
	 * SessionManagerを返します。
	 * 
	 * @return
	 */
	public SessionManager getSessionManager() {
		return sessionManager;
	}

	/**
	 * SDLoaderのバージョンを返します。
	 * 
	 * @return
	 */
	public static String getVersion() {
		try {
			InputStream is = SDLoader.class
					.getResourceAsStream("/META-INF/sdloaderversion.txt");
			Properties p = new Properties();
			p.load(is);
			return p.getProperty("libversion");
		} catch (IOException ioe) {
			return null;
		}
	}

	/**
	 * イベントリスナーを追加します。
	 * 
	 * @param type
	 * @param listener
	 */
	public void addEventListener(String type, LifecycleListener listener) {
		dispatcher.addEventListener(type, listener);
	}

	/**
	 * サーバ起動中かどうかを返します。
	 */
	public boolean isRunning() {
		return running.get();
	}

	/**
	 * ソケットをオープンし、サーバを開始します。
	 */
	public void start() {
		final long t = System.currentTimeMillis();

		checkNotRunning();

		helper.printInitMessage(log);

		dispatcher.dispatchEvent(new LifecycleEvent<SDLoader>(
				LifecycleEvent.BEFORE_START, this));

		ServerSocket initSocket = null;

		final Thread currentThread = Thread.currentThread();
		final ClassLoader currentContextClassLoader = currentThread
				.getContextClassLoader();
		try {
			currentThread
					.setContextClassLoader(SDLoader.class.getClassLoader());

			initConfig();

			initSocket = initServerSocket();

			initSessionManager();

			initWebApp();

			sdLoaderThread = new SDLoaderThread(this, initSocket);

			sdLoaderThread.start();
		} catch (RuntimeException re) {
			IOUtil.closeServerSocketNoException(initSocket);
			throw re;
		} finally {
			currentThread.setContextClassLoader(currentContextClassLoader);
		}

		log.info("SDLoader[port:" + getPort() + "] startup in "
				+ (System.currentTimeMillis() - t) + " ms.");

		running.set(true);

		dispatcher.dispatchEvent(new LifecycleEvent<SDLoader>(
				LifecycleEvent.AFTER_START, this));
	}

	/**
	 * ソケットを閉じ、サーバを終了します。
	 */
	public void stop() {
		if (running.getAndSet(false) == false) {
			return;
		}
		sdLoaderThread.close();
		ThreadUtil.join(sdLoaderThread);
		if (isExitOnStop()) {
			System.exit(0);
		}
	}

	/**
	 * SDLoaderが終了するまで待機します。
	 * 
	 */
	public void waitForStop() {
		if (isRunning()) {
			ThreadUtil.join(sdLoaderThread);
		}
	}

	protected void initConfig() {
		// init home
		String homeDir = config.getConfigStringIgnoreExist(KEY_SDLOADER_HOME);
		if (homeDir == null) {
			homeDir = System.getProperty(KEY_SDLOADER_HOME);
			if (homeDir == null) {
				homeDir = System.getProperty("user.dir");
			}
		}
		homeDir = PathUtil.replaceFileSeparator(homeDir);
		homeDir = PathUtil.removeEndSlashIfNeed(homeDir);

		config.setConfig(KEY_SDLOADER_HOME, homeDir);
		log.info("SDLOADER_HOME=" + homeDir);

		// init webappDir
		String webAppsDirPath = config
				.getConfigStringIgnoreExist(KEY_SDLOADER_WEBAPPS_DIR);
		if (webAppsDirPath == null) {
			webAppsDirPath = System.getProperty(KEY_SDLOADER_WEBAPPS_DIR);
			if (webAppsDirPath == null) {
				webAppsDirPath = "webapps";
			}
		}
		webAppsDirPath = PathUtil.replaceFileSeparator(webAppsDirPath);
		if (!PathUtil.isAbsolutePath(webAppsDirPath)) {
			// homeからの絶対パスに変換
			webAppsDirPath = homeDir + "/" + webAppsDirPath;
		}
		config.setConfig(KEY_SDLOADER_WEBAPPS_DIR, webAppsDirPath);
		log.info(KEY_SDLOADER_WEBAPPS_DIR + "=" + webAppsDirPath);

		// init jsp lib path
		config.setConfigFromSystemIfNotExit(KEY_SDLOADER_JSP_LIBPATH);

		// init work dir
		String workDir = config
				.getConfigStringIgnoreExist(KEY_SDLOADER_WORK_DIR);
		if (workDir == null) {
			workDir = System.getProperty("java.io.tmpdir") + "/.sdloader";
		}
		config.setConfig(KEY_SDLOADER_WORK_DIR, workDir);
	}

	protected void initSessionManager() {
		String sessionManagerClassName = config
				.getConfigString(KEY_SDLOADER_SESSION_MANAGER);
		this.sessionManager = ClassUtil.newInstance(sessionManagerClassName);
	}

	protected void initWebApp() {
		try {
			log.info("InternalWebApplication initialize start.");
			webAppManager.init(config);
			log.info("InternalWebApplication initialize success.");
		} catch (RuntimeException e) {
			log.error("InternalWebApplication initialize fail.", e);
			throw e;
		}
	}

	protected ServerSocket initServerSocket() {

		final int port = getPort();
		final boolean useOutSidePort = config
				.getConfigBoolean(KEY_SDLOADER_USE_OUTSIDE_PORT);
		final boolean autoPortDetect = config
				.getConfigBoolean(KEY_SDLOADER_AUTO_PORT_DETECT);
		final boolean sslEnable = config
				.getConfigBoolean(KEY_SDLOADER_SSL_ENABLE);

		String portMessage = autoPortDetect ? "AutoDetect" : String
				.valueOf(port);
		log.info("Bind start. Port=" + portMessage + " useOutSidePort="
				+ useOutSidePort + " SSL=" + sslEnable);

		ServerSocket servetSocket = null;
		try {
			try {
				servetSocket = helper.createServerSocket(port, sslEnable,
						useOutSidePort, false);
			} catch (IOException ioe) {
				if (autoPortDetect) {
					servetSocket = helper.createServerSocket(0, sslEnable,
							useOutSidePort, false);
				} else {
					// 同一プロセス内で閉じていた場合、ポートをSO_RESUEADDRで開く
					boolean reuse = false;
					if (IOUtil.isClosedPort(port)) {
						reuse = true;
					} else {
						// try to stop SDLoader
						reuse = helper.tryConnectAndSendStopCommand(port);
					}
					if (reuse) {
						log.info("Reuse address.Port=" + port);
						servetSocket = helper.createServerSocket(port,
								sslEnable, useOutSidePort, reuse);
					} else {
						throw ioe;
					}
				}
			}
			int bindSuccessPort = servetSocket.getLocalPort();
			config.setConfig(KEY_SDLOADER_PORT, bindSuccessPort);
			IOUtil.setPortStatus(bindSuccessPort, true);
			log.info("Bind success. Port=" + getPort());
		} catch (IOException ioe) {
			log.error("Bind fail! Port=" + portMessage, ioe);
			throw new RuntimeException(ioe);
		}
		return servetSocket;
	}

	protected boolean checkNotRunning() {
		if (isRunning()) {
			throw new IllegalStateException("SDLoader is already running.");
		}
		return false;
	}

	public WebAppManager getWebAppManager() {
		return webAppManager;
	}

	static class SDLoaderThread extends Thread {

		private AtomicBoolean running = new AtomicBoolean();

		private ServerSocket serverSocket;

		private HttpProcessorPool socketProcessorPool;

		private SDLoader sdLoader;

		public SDLoaderThread(SDLoader sdLoader, ServerSocket serverSocket) {
			super("SDLoaderThread");
			setDaemon(false);

			this.sdLoader = sdLoader;
			this.serverSocket = serverSocket;
			initSocketProcessor();
			running.set(true);
		}

		protected void initSocketProcessor() {
			int maxThreadPoolNum = sdLoader.getSDLoaderConfig()
					.getConfigInteger(KEY_SDLOADER_MAX_THREAD_POOL_NUM);
			this.socketProcessorPool = new HttpProcessorPool(sdLoader,
					maxThreadPoolNum);
		}

		public boolean isRunning() {
			return running.get();
		}

		public void run() {
			while (isRunning()) {
				Socket socket = null;
				try {
					socket = serverSocket.accept();
					log.debug("Accept socket connection.");
					HttpProcessor con = socketProcessorPool.borrowProcessor();
					con.process(socket);
				} catch (AccessControlException ace) {
					log.warn("Socket accept security exception "
							+ ace.getMessage(), ace);
					break;
				} catch (Exception ioe) {
					if (isRunning() == false) {
						break;
					} else {
						log.error("Socket accept error.", ioe);
					}
				}
			}
			shutdownSDLoader();
		}

		public void close() {
			synchronized (this) {
				if (running.getAndSet(false) == true) {
					IOUtil.closeServerSocketNoException(serverSocket);
					serverSocket = null;
				}
			}
		}

		protected void shutdownSDLoader() {
			int port = sdLoader.getPort();
			SDLoader.log.info("SDLoader[port:" + port + "] shutdown start.");

			sdLoader.dispatcher.dispatchEvent(new LifecycleEvent<SDLoader>(
					LifecycleEvent.BEFORE_STOP, sdLoader));

			// destroy webapps
			IOUtil.closeServerSocketNoException(serverSocket);
			socketProcessorPool.close();
			sdLoader.webAppManager.close();
			sdLoader.sessionManager.close();
			sdLoader.shutdownHook.removeShutdownHook();

			sdLoader.dispatcher.dispatchEvent(new LifecycleEvent<SDLoader>(
					LifecycleEvent.AFTER_STOP, sdLoader));

			SDLoader.log.info("SDLoader[port:" + port + "] stop.");
		}
	}
}
