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
package sdloader.javaee;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import sdloader.SDLoader;
import sdloader.constants.SDLoaderConstants;
import sdloader.http.HttpResponse;
import sdloader.internal.SDLoaderConfig;
import sdloader.internal.resource.ArchiveTypeResource;
import sdloader.internal.resource.BranchTypeResource;
import sdloader.internal.resource.LeafTypeResource;
import sdloader.internal.resource.Resource;
import sdloader.internal.resource.ResourceBuilder;
import sdloader.internal.resource.ResourceBuilderImpl;
import sdloader.internal.resource.ResourceURLConnection;
import sdloader.javaee.classloader.ClassLoaderHandler;
import sdloader.javaee.classloader.DevWebAppClassLoader;
import sdloader.javaee.classloader.InMemoryWebAppClassLoader;
import sdloader.javaee.classloader.WebAppClassLoader;
import sdloader.javaee.jasper.InMemoryEmbeddedServletOptions;
import sdloader.javaee.servlet.CommandServlet;
import sdloader.javaee.servlet.FileSavingServlet;
import sdloader.javaee.servlet.WebAppListServlet;
import sdloader.javaee.webxml.InitParamTag;
import sdloader.javaee.webxml.ServletMappingTag;
import sdloader.javaee.webxml.ServletTag;
import sdloader.javaee.webxml.WebAppTag;
import sdloader.javaee.webxml.WebXml;
import sdloader.javaee.webxml.WebXmlBuilder;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.Assertion;
import sdloader.util.ClassUtil;
import sdloader.util.CollectionsUtil;
import sdloader.util.IOUtil;
import sdloader.util.MessageDigestUtil;
import sdloader.util.PathUtil;
import sdloader.util.ResourceUtil;
import sdloader.util.WarUtil;
import sdloader.util.WebUtil;

/**
 * <pre>
 * Webアプリケーションのマネージャークラス
 * webapps以下のWebアプリケーションを読み込み、 初期化します。
 * 
 * webアプリは、4つの方法で配置できます。 
 *  ・warファイル 
 *  ・ディレクトリ
 *  ・xmlファイル
 *  ・WebAppContext
 * warファイルは、ファイル名と同じディレクトリがない場合はその場で解凍されます。
 * warファイル名がコンテキストパスとなります。
 * 
 * ディレクトリは、ディレクトリ名がコンテキストパスになります。
 * 
 * xmlファイルは、コンテキストパスとドキュメントベースを指定できます。 
 * ファイルに&lt;Context path=&quot;/コンテキストパス&quot; docBase=&quot;Webアプリのドキュメントベース&quot;/&gt;
 *  という内容でファイルを記述すると、そこからロードを行います。
 * コンテキストパスがない場合は、xmlファイル名がコンテキストパスになります。
 * docBaseは、.からはじめると相対パス、そうでない場合は絶対パスとして取り扱います。
 * 相対パスの場合は、xmlファイルのある位置からの相対パスを記述します。
 * 
 * WebAppContextを使用する場合は、WebAppContextをインスタンス化後、addWebAppContext()を使い
 * アプリケーションを登録します。
 * 
 * </pre>
 * 
 * @author c9katayama
 * @author shot
 */
public class WebAppManager {

	private static SDLoaderLog log = SDLoaderLogFactory
			.getLog(WebAppManager.class);

	protected String webappDirPath;

	protected List<WebAppContext> webAppContextList = CollectionsUtil
			.newArrayList();

	protected List<InternalWebApplication> webAppList = CollectionsUtil
			.newArrayList();

	protected boolean isInmemoryExtract = false;

	protected Map<URL, Map<URL, Resource>> warInmemoryMap = CollectionsUtil
			.newHashMap();

	private static final String JASPER_SERVLET_CLASS = "org.apache.jasper.servlet.JspServlet";

	private static final boolean JASPER_SUPPORT;

	private boolean initialized;

	private SDLoaderConfig config;

	static {
		JASPER_SUPPORT = ClassUtil.hasClass(JASPER_SERVLET_CLASS);
	}

	public void init(SDLoaderConfig config) {
		this.config = config;
		try {
			initConfig();
			if (webAppContextList.size() == 0) {
				detectWebApps();
			}
			initWebAppContext();
			initialized = true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected void initConfig() {
		isInmemoryExtract = config
				.getConfigBoolean(SDLoader.KEY_SDLOADER_WAR_INMEMORY_EXTRACT);
	}

	protected void detectWebApps() throws Exception {
		this.webappDirPath = config
				.getConfigString(SDLoader.KEY_SDLOADER_WEBAPPS_DIR);
		File webappDir = new File(webappDirPath);
		if (!webappDir.exists()) {
			log.info("webapps directory not exists.path=" + webappDirPath);
			return;
		}

		File[] dirs = webappDir.listFiles(IOUtil.IGNORE_DIR_FILEFILTER);
		File[] warFiles = webappDir.listFiles(IOUtil.WAR_FILEFILETR);

		if (!isInmemoryExtract) {
			if (warFiles != null) {
				for (int i = 0; i < warFiles.length; i++) {
					WarUtil.extractWar(warFiles[i], new File(webappDir, WarUtil
							.getArchiveName(warFiles[i].getName())));
				}
			}
		}
		// webapps以下のフォルダ
		dirs = webappDir.listFiles(IOUtil.IGNORE_DIR_FILEFILTER);
		if (!isInmemoryExtract) {
			if (dirs != null) {
				for (int i = 0; i < dirs.length; i++) {
					final String contextPath = "/" + dirs[i].getName();
					final String docBase = webappDirPath + contextPath;
					final WebAppContext context = new WebAppContext(
							contextPath, PathUtil.file2URL(docBase));
					addWebAppContext(context);
					log.info("detect webapp context. contextPath="
							+ contextPath + " docBase=" + docBase);
				}
			}
		} else {
			URL
					.setURLStreamHandlerFactory(new ArchiveURLStreamHandlerFactory());
			ResourceBuilder builder = new ResourceBuilderImpl();
			for (File warfile : warFiles) {
				final URL warDocRoot = ResourceUtil
						.createURL(WarURLStreamHandler.PROTOCOL + ":"
								+ warfile.toURI().toURL().toExternalForm());
				final String contextPath = "/"
						+ ResourceUtil.stripExtension(warfile.getName());
				Map<URL, Resource> map = builder.build(warfile.getPath());
				WebAppContext context = new WebAppContext(contextPath,
						warDocRoot);
				addWebAppContext(context);
				warInmemoryMap.put(warDocRoot, map);
			}
		}
		// コンテキストXML
		File[] contextXMLs = webappDir.listFiles(IOUtil.XML_FILEFILTER);
		parseContextXMLs(contextXMLs);
	}

	protected void initWebAppContext() throws Exception {
		if (webAppContextList.size() == 0) {
			throw new RuntimeException("There are no Web Application.");
		}
		for (WebAppContext context : webAppContextList) {
			initWebAppContext0(context);
		}
		this.webAppList.add(getRootWebApplication());
		this.webAppList.add(getCommandWebApplication());
		// コンテキストパスの長い順にソート
		Collections.sort(webAppList, new Comparator<InternalWebApplication>() {
			public int compare(InternalWebApplication o1,
					InternalWebApplication o2) {
				return o2.getContextPath().compareTo(o1.getContextPath());
			}
		});
	}

	protected void initWebAppContext0(WebAppContext context) throws Exception {
		final String contextPath = context.getContextPath();
		log.info("create webapp [" + contextPath + "]");

		final URL[] docBase = context.getDocBase();
		// docBaseがwarファイルだった場合、テンポラリに展開してそのパスを使用
		for (int i = docBase.length - 1; i >= 0; i--) {
			URL url = docBase[i];
			File file = PathUtil.url2File(url);
			if (file.exists() && file.isFile()
					&& file.getName().endsWith(".war")) {
				File extractDir = new File(generateWorkDirPath("war",
						contextPath + "_" + file.getName()));
				WarUtil.extractWar(file, extractDir);
				docBase[i] = PathUtil.file2URL(extractDir);
			}
		}
		context.setDocBase(docBase);

		WebXml webxml = context.getWebXml();
		if (webxml == null) {
			webxml = buildWebXml(docBase);
		}
		setDefaultServlet(webxml, docBase, contextPath, isInmemoryExtract);
		// create InternalWebApplication
		ClassLoader webAppClassLoader = null;
		if (isInmemoryExtract == false) {
			webAppClassLoader = createWebAppClassLoader(docBase, context
					.getAdditionalPathList(), context.getClassLoaderHandler());
		} else {
			webAppClassLoader = createInMemoryWebAppClassLoader(docBase[0]);
		}
		context.setWebXml(webxml);
		InternalWebApplication webapp = new InternalWebApplication(context,
				webAppClassLoader, this);
		this.webAppList.add(webapp);

		log.info("create success.");
	}

	protected WebXml buildWebXml(URL[] url) throws SAXException,
			ParserConfigurationException, IOException, MalformedURLException {
		for (int i = 0; i < url.length; i++) {
			URL webXmlUrl = ResourceUtil.createURL(url[i], "WEB-INF/web.xml");
			if (ResourceUtil.isResourceExist(webXmlUrl)) {
				log.info("web.xml load start. Path=" + webXmlUrl);
				WebXml webXml = WebXmlBuilder.build(webXmlUrl);
				log.info("web.xml load success.");
				return webXml;
			}
		}
		log.warn("web.xml not detected. use default web.xml.");
		// default
		WebXml webxml = new WebXml();
		return webxml;
	}

	protected InternalWebApplication getRootWebApplication() {
		// InternalWebApplication List
		final String webAppListServletName = "webAppList";
		final ServletTag webAppListServletTag = new ServletTag();
		webAppListServletTag.setLoadOnStartup(0);
		webAppListServletTag.setServletClass(WebAppListServlet.class.getName());
		webAppListServletTag.setServletName(webAppListServletName);

		final ServletMappingTag webAppListMappingTag = new ServletMappingTag();
		webAppListMappingTag.setServletName(webAppListServletName);
		webAppListMappingTag.setUrlPattern("/");

		final WebXml webXmlTag = new WebXml();
		WebAppTag webAppTag = webXmlTag.getWebApp();
		webAppTag.addServlet(webAppListServletTag);
		webAppTag.addServletMapping(webAppListMappingTag);
		webXmlTag.setWebApp(webAppTag);

		// Default servlet
		String contextPath = "/";
		URL docBase = PathUtil.file2URL(webappDirPath + "/"
				+ SDLoaderConstants.ROOT_DIR_NAME);
		if (ResourceUtil.isResourceExist(docBase)) {
			setDefaultServlet(webXmlTag, new URL[] { docBase }, contextPath,
					false);
		}
		final ClassLoader webAppClassLoader = createWebAppClassLoader(
				new URL[] { docBase }, null, null);
		WebAppContext context = new WebAppContext(contextPath, docBase);
		context.setWebXml(webXmlTag);
		final InternalWebApplication webapp = new InternalWebApplication(
				context, webAppClassLoader, this);

		return webapp;
	}

	private InternalWebApplication getCommandWebApplication() {
		// Command servlet
		String contextPath = "/sdloader-command";
		URL docBase = PathUtil.file2URL(webappDirPath + "/sdloader-control");
		WebXml webXmlTag = new WebXml();
		webXmlTag.getWebApp().addServlet(
				new ServletTag().setServletName("sdloader-command")
						.setServletClass(CommandServlet.class));
		webXmlTag.getWebApp().addServletMapping(
				new ServletMappingTag().setServletName("sdloader-command")
						.setUrlPattern("/*"));

		final ClassLoader webAppClassLoader = createWebAppClassLoader(
				new URL[] { docBase }, null, null);
		WebAppContext context = new WebAppContext(contextPath, docBase);
		context.setWebXml(webXmlTag);
		final InternalWebApplication webapp = new InternalWebApplication(
				context, webAppClassLoader, this);

		return webapp;
	}

	/**
	 * Webアプリ用のクラスローダーを生成します。
	 * 
	 * @param absoluteContextPath
	 * @return
	 * @throws MalformedURLException
	 */
	protected ClassLoader createWebAppClassLoader(URL[] docBase,
			List<WebAppContext.Path> pathList,
			ClassLoaderHandler classLoaderHandler) {
		List<URL> urlList = CollectionsUtil.newArrayList();

		// additional path
		if (pathList != null) {
			for (WebAppContext.Path path : pathList) {
				File pathDir = PathUtil.url2File(path.getPath());
				if (pathDir.exists()) {
					if (path.isLibPath()) {
						URL[] libs = WebUtil.createClassPaths(pathDir,
								IOUtil.JAR_ZIP_FILEFILTER, false);
						if (libs != null) {
							for (int j = 0; j < libs.length; j++) {
								urlList.add(libs[j]);
							}
						}
					} else {
						urlList.add(path.getPath());
					}
				}
			}
		}
		// classes
		for (int i = 0; i < docBase.length; i++) {
			File docBaseDir = PathUtil.url2File(docBase[i]);
			File classesDir = new File(docBaseDir, "/WEB-INF/classes/");
			if (classesDir.exists()) {
				urlList.add(PathUtil.file2URL(classesDir));
			}
			// WEB-INF/lib
			File webinfLibDir = new File(docBaseDir, "/WEB-INF/lib");
			URL[] libs = WebUtil.createClassPaths(webinfLibDir,
					IOUtil.JAR_ZIP_FILEFILTER, false);
			if (libs != null) {
				for (int j = 0; j < libs.length; j++) {
					urlList.add(libs[j]);
				}
			}
		}

		URL[] urls = (URL[]) urlList.toArray(new URL[] {});
		ClassLoader parentClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (log.isDebugEnabled() || classLoaderHandler != null) {
			return new DevWebAppClassLoader(urls, parentClassLoader,
					classLoaderHandler);
		} else {
			return new WebAppClassLoader(urls, parentClassLoader);
		}
	}

	/**
	 * inMemory動作のWebアプリ用のクラスローダーを生成します。
	 * メモリ上のwarファイルから、クラスパスになるリソースを読み込み、ClassLoaderに 設定します。
	 * 
	 * @param absoluteContextPath
	 * @return
	 * @throws MalformedURLException
	 */
	protected ClassLoader createInMemoryWebAppClassLoader(URL docBase) {
		List<URL> urlList = CollectionsUtil.newArrayList();
		Map<URL, Resource> resourceMap = warInmemoryMap.get(docBase);
		// classes
		URL classesDir = ResourceUtil.createURL(docBase, "WEB-INF/classes/");
		if (resourceMap.get(classesDir) != null) {
			urlList.add(classesDir);
		}
		// WEB-INF/lib
		URL webinfLibDir = ResourceUtil.createURL(docBase, "WEB-INF/lib/");
		Resource libDirResource = resourceMap.get(webinfLibDir);
		if (libDirResource != null) {
			BranchTypeResource dirResource = (BranchTypeResource) libDirResource;
			List<Resource> libs = dirResource.getResources();
			if (libs != null) {
				for (Resource lib : libs) {
					String path = lib.getPath();
					if (lib instanceof LeafTypeResource
							&& (path.endsWith(".jar") || path.endsWith(".zip"))) {
						urlList.add(lib.getURL());
					}
				}
			}
		}
		URL[] urls = (URL[]) urlList.toArray(new URL[] {});
		ClassLoader parentClassLoader = Thread.currentThread()
				.getContextClassLoader();
		WebAppClassLoader webAppClassLoader = new InMemoryWebAppClassLoader(
				resourceMap, urls, parentClassLoader);

		return webAppClassLoader;
	}

	/**
	 * デフォルトサーブレット（FileSavingServlet)の記述を追加します。
	 * 
	 * @param webxml
	 * @param absoluteContextPath
	 *            ドキュメントルートになるパス
	 * @param contextPath
	 *            コンテキストパス
	 * @param warInMemory
	 */
	private void setDefaultServlet(WebXml webXml, URL[] docBase,
			String contextPath, boolean warInMemory) {

		// jsp compiler servlet
		final String jspServletName = "jsp";
		if (JASPER_SUPPORT
				&& webXml.getWebApp().findServlet(jspServletName) == null) {
			log.debug("set default jsp servlet.");
			ServletTag jspServlet = new ServletTag();
			jspServlet.setLoadOnStartup(0);
			jspServlet.setServletClass(JASPER_SERVLET_CLASS);
			jspServlet.setServletName(jspServletName);
			jspServlet.addInitParam(new InitParamTag("logVerbosityLevel",
					"WARNING"));
			jspServlet.addInitParam(new InitParamTag("validating", "false"));
			jspServlet.addInitParam(new InitParamTag("fork", "false"));

			// inmemory用TldLocationCache利用の為
			if (isInmemoryExtract) {
				jspServlet.addInitParam(new InitParamTag("engineOptionsClass",
						InMemoryEmbeddedServletOptions.class.getName()));
			}
			// JSPコンパイルディレクトリの作成
			String jspWorkDirPath = generateWorkDirPath("jsp", contextPath);
			File jspWorkDir = new File(jspWorkDirPath);
			jspWorkDir.mkdirs();
			jspServlet.addInitParam(new InitParamTag("scratchdir",
					jspWorkDirPath));

			String jspLibPath = config
					.getConfigStringIgnoreExist(SDLoader.KEY_SDLOADER_JSP_LIBPATH);
			if (jspLibPath != null) {
				jspLibPath = PathUtil.replaceFileSeparator(jspLibPath);
				jspServlet.addInitParam(new InitParamTag("classpath",
						jspLibPath));
			}

			ServletMappingTag jspMapping = new ServletMappingTag();
			jspMapping.setServletName(jspServletName);
			jspMapping.setUrlPattern("*.jsp");

			webXml.getWebApp().addServlet(jspServlet);
			webXml.getWebApp().addServletMapping(jspMapping);
		}

		// default servlet (file saving)
		final String fileSavingServletName = "default";
		if (webXml.getWebApp().findServlet(fileSavingServletName) == null) {
			log.debug("set default file savings servlet.");
			ServletTag fileServletTag = new ServletTag();
			fileServletTag.setLoadOnStartup(0);
			// TODO 統合したい
			if (warInMemory) {
				fileServletTag
						.setServletClass("sdloader.javaee.servlet.InMemoryFileSavingServlet");
			} else {
				fileServletTag
						.setServletClass("sdloader.javaee.servlet.FileSavingServlet");
			}
			fileServletTag.setServletName(fileSavingServletName);
			String docRoots = "";
			for (int i = 0; i < docBase.length; i++) {
				if (i != 0) {
					docRoots += ",";
				}
				docRoots += docBase[i].toExternalForm();
			}
			fileServletTag.addInitParam(new InitParamTag(
					FileSavingServlet.PARAM_DOC_ROOT, docRoots));
			// noCacheMode
			boolean noCacheMode = config
					.getConfigBoolean(HttpResponse.KEY_RESPONSE_USE_NOCACHE_MODE);
			if (noCacheMode) {
				fileServletTag.addInitParam(new InitParamTag(
						FileSavingServlet.PARAM_IGNORE_LAST_MODIFIED, "true"));
			}

			ServletMappingTag mappingTag = new ServletMappingTag();
			mappingTag.setServletName(fileSavingServletName);
			mappingTag.setUrlPattern("/*");

			webXml.getWebApp().addServlet(fileServletTag);
			webXml.getWebApp().addServletMapping(mappingTag);
		}
	}

	/**
	 * 作業用ディレクトリパスを返します。
	 * 
	 * @param functionName
	 *            機能名（ｊｓｐやwarなど）
	 * @param contextPath
	 * @return
	 */
	protected String generateWorkDirPath(String functionName, String contextPath) {
		// クラスパスを使って、ハッシュを作成
		String genDir = MessageDigestUtil.digest(System
				.getProperty("java.class.path"));
		String workRoot = config
				.getConfigString(SDLoader.KEY_SDLOADER_WORK_DIR);
		workRoot = PathUtil.replaceFileSeparator(workRoot);
		String workDirPath = PathUtil.jointPathWithSlash(workRoot, genDir);
		workDirPath = PathUtil.jointPathWithSlash(workDirPath, functionName);
		workDirPath = PathUtil.jointPathWithSlash(workDirPath, contextPath);
		return workDirPath;
	}

	protected void parseContextXMLs(File[] contextXMLs) throws Exception {
		if (contextXMLs != null && contextXMLs.length > 0) {
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			for (int i = 0; i < contextXMLs.length; i++) {
				final File contextXml = contextXMLs[i];
				final String fileName = contextXml.getName();
				WebAppContextXmlParserHandler contextXmlParserHandler = new WebAppContextXmlParserHandler(
						fileName, webappDirPath);
				parser.parse(contextXml, contextXmlParserHandler);
				addWebAppContext(Assertion.notNull(contextXmlParserHandler
						.getWebAppContext()));
			}
		}
	}

	/**
	 * /から始まるコンテキストパスを返します。
	 * 
	 * @return
	 */
	public List<String> getContextPathList() {
		List<String> contextPathList = CollectionsUtil.newArrayList();
		for (WebAppContext context : webAppContextList) {
			contextPathList.add(context.getContextPath());
		}
		return contextPathList;
	}

	/**
	 * WebAppのリストを返します。
	 * 
	 * @return
	 */
	public List<InternalWebApplication> getWebAppList() {
		return webAppList;
	}

	/**
	 * 初期化後かどうか
	 * 
	 * @return
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * WebAppContextを追加します。
	 * 
	 * @param context
	 */
	public void addWebAppContext(WebAppContext context) {
		this.webAppContextList.add(context);
	}

	/**
	 * コンテキストパスに対するWebAppを検索します。
	 * 
	 * @param requestURI
	 * @return 該当するWebAppがない場合、null
	 */
	public InternalWebApplication findWebApp(final String requestURI) {
		InternalWebApplication app = findWebApp0(requestURI);
		if (app == null) {
			// /text.txt などのROOTの場合
			app = findWebApp0("/");
		}
		return app;
	}

	protected InternalWebApplication findWebApp0(final String requestURI) {
		for (Iterator<InternalWebApplication> itr = getWebAppList().iterator(); itr
				.hasNext();) {
			InternalWebApplication webapp = itr.next();
			String contextPath = webapp.getContextPath();
			if (requestURI.equals(contextPath)) {
				return webapp;
			}
			if (requestURI.startsWith(contextPath)) {
				String contextStripPath = requestURI.substring(contextPath
						.length());
				if (contextStripPath.startsWith("/")) {
					return webapp;
				}
			}
		}
		return null;
	}

	public void close() {
		List<InternalWebApplication> webAppList = getWebAppList();
		for (Iterator<InternalWebApplication> itr = webAppList.iterator(); itr
				.hasNext();) {
			InternalWebApplication webapp = itr.next();
			List<Filter> filterList = webapp.getFilterList();
			if (filterList != null) {
				for (Filter filter : filterList) {
					try {
						synchronized (filter) {
							filter.destroy();
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			List<Servlet> servletList = webapp.getServletList();
			if (servletList != null) {
				for (Servlet servlet : servletList) {
					try {
						synchronized (servlet) {
							servlet.destroy();
						}
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			ServletContextEvent contextEvent = new ServletContextEvent(webapp
					.getServletContext());
			webapp.getListenerEventDispatcher()
					.dispatchServletContextListener_contextDestroyed(
							contextEvent);
			itr.remove();
		}
	}

	private final class ArchiveURLStreamHandlerFactory implements
			URLStreamHandlerFactory {
		public URLStreamHandler createURLStreamHandler(final String protocol) {
			if (protocol.equals(WarURLStreamHandler.PROTOCOL)) {
				return new WarURLStreamHandler();
			} else if (protocol.equals(InnerJarURLStreamHandler.PROTOCOL)) {
				return new InnerJarURLStreamHandler();
			}
			return null;
		}
	}

	private final class WarURLStreamHandler extends URLStreamHandler {
		public static final String PROTOCOL = "war";

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			String warPath = u.toExternalForm();
			warPath = warPath.substring(0, warPath.indexOf("!"));
			Map<URL, Resource> resources = null;
			for (URL key : warInmemoryMap.keySet()) {
				if (key.toExternalForm().equals(warPath)) {
					resources = warInmemoryMap.get(key);
					break;
				}
			}
			final Resource resource = (resources != null) ? resources.get(u)
					: null;
			return new ResourceURLConnection(resource);
		}
	}

	private final class InnerJarURLStreamHandler extends URLStreamHandler {
		public static final String PROTOCOL = "innerjar";

		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			String warPath = u.getPath();
			warPath = warPath.substring(0, warPath.indexOf("!"));
			Map<URL, Resource> resources = null;
			for (URL key : warInmemoryMap.keySet()) {
				if (key.toExternalForm().equals(warPath)) {
					resources = warInmemoryMap.get(key);
					break;
				}
			}
			if (resources == null) {
				return new ResourceURLConnection(null);
			}
			String jarPath = u.toExternalForm();
			String innerJar = jarPath.substring(0, jarPath.lastIndexOf("!"));
			String resourceName = jarPath.substring(
					jarPath.lastIndexOf("!/") + 2, jarPath.length());
			Resource jarResource = resources.get(ResourceUtil
					.createURL(innerJar));
			if (jarResource != null) {
				ArchiveTypeResource jar = (ArchiveTypeResource) jarResource;
				final Resource innerJarResource = jar
						.getArchiveResource(resourceName);
				if (innerJarResource != null) {
					return new ResourceURLConnection(innerJarResource);
				}
			}
			return new ResourceURLConnection(null);
		}
	}
}
