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
package sdloader.javaee.jasper;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagLibraryInfo;

import org.apache.jasper.Constants;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.xmlparser.ParserUtils;
import org.apache.jasper.xmlparser.TreeNode;
import org.xml.sax.InputSource;

import sdloader.internal.resource.ArchiveTypeResource;
import sdloader.internal.resource.BranchTypeResource;
import sdloader.internal.resource.DirectoryTypeResource;
import sdloader.internal.resource.LeafTypeResource;
import sdloader.internal.resource.Resource;
import sdloader.javaee.classloader.InMemoryWebAppClassLoader;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;

/**
 * InMemory動作用のTldLocationCache
 *
 * @author c9katayama
 */
@SuppressWarnings("unchecked")
@Deprecated
public class InMemoryTldLocationsCache extends TldLocationsCache {

	private static SDLoaderLog log = SDLoaderLogFactory
			.getLog(InMemoryTldLocationsCache.class);

	private static final String WEB_XML = "/WEB-INF/web.xml";
	private static final String FILE_PROTOCOL = "file:";
	private static final String JAR_FILE_SUFFIX = ".jar";

	private Hashtable<String, String[]> mappings;

	private boolean initialized;
	private ServletContext ctxt;

	public InMemoryTldLocationsCache(ServletContext ctxt) {
		this(ctxt, true);
	}

	/**
	 * Constructor.
	 *
	 * @param ctxt
	 *            the servlet context of the web application in which Jasper is
	 *            running
	 * @param redeployMode
	 *            if true, then the compiler will allow redeploying a tag
	 *            library from the same jar, at the expense of slowing down the
	 *            server a bit. Note that this may only work on JDK 1.3.1_01a
	 *            and later, because of JDK bug 4211817 fixed in this release.
	 *            If redeployMode is false, a faster but less capable mode will
	 *            be used.
	 */
	public InMemoryTldLocationsCache(ServletContext ctxt, boolean redeployMode) {
		super(ctxt, redeployMode);
		this.ctxt = ctxt;
		mappings = new Hashtable<String, String[]>();
		initialized = false;
	}

	/**
	 * Gets the 'location' of the TLD associated with the given taglib 'uri'.
	 *
	 * Returns null if the uri is not associated with any tag library 'exposed'
	 * in the web application. A tag library is 'exposed' either explicitly in
	 * web.xml or implicitly via the uri tag in the TLD of a taglib deployed in
	 * a jar file (WEB-INF/lib).
	 *
	 * @param uri
	 *            The taglib uri
	 *
	 * @return An array of two Strings: The first element denotes the real path
	 *         to the TLD. If the path to the TLD points to a jar file, then the
	 *         second element denotes the name of the TLD entry in the jar file.
	 *         Returns null if the uri is not associated with any tag library
	 *         'exposed' in the web application.
	 */
	public String[] getLocation(String uri) throws JasperException {
		if (!initialized) {
			init();
		}
		return (String[]) mappings.get(uri);
	}

	private void init() throws JasperException {
		if (initialized)
			return;
		try {
			processWebDotXml();
			scanWebInf();
			initialized = true;
		} catch (Exception ex) {
			throw new JasperException(Localizer.getMessage(
					"jsp.error.internal.tldinit", ex.getMessage()));
		}
	}

	/*
	 * Populates taglib map described in web.xml.
	 */
	private void processWebDotXml() throws Exception {

		InputStream is = null;

		try {
			// Acquire input stream to web application deployment descriptor
			String altDDName = (String) ctxt
					.getAttribute(Constants.ALT_DD_ATTR);
			URL uri = null;
			if (altDDName != null) {
				try {
					uri = new URL(FILE_PROTOCOL + altDDName.replace('\\', '/'));
				} catch (MalformedURLException e) {
					if (log.isWarnEnabled()) {
						log.warn(Localizer.getMessage(
								"jsp.error.internal.filenotfound", altDDName));
					}
				}
			} else {
				uri = ctxt.getResource(WEB_XML);
				if (uri == null && log.isWarnEnabled()) {
					log.warn(Localizer.getMessage(
							"jsp.error.internal.filenotfound", WEB_XML));
				}
			}

			if (uri == null) {
				return;
			}
			is = uri.openStream();
			InputSource ip = new InputSource(is);
			ip.setSystemId(uri.toExternalForm());

			// Parse the web application deployment descriptor
			TreeNode webtld = null;
			// altDDName is the absolute path of the DD
			if (altDDName != null) {
				webtld = new ParserUtils().parseXMLDocument(altDDName, ip);
			} else {
				webtld = new ParserUtils().parseXMLDocument(WEB_XML, ip);
			}

			// Allow taglib to be an element of the root or jsp-config (JSP2.0)
			TreeNode jspConfig = webtld.findChild("jsp-config");
			if (jspConfig != null) {
				webtld = jspConfig;
			}
			Iterator taglibs = webtld.findChildren("taglib");
			while (taglibs.hasNext()) {

				// Parse the next <taglib> element
				TreeNode taglib = (TreeNode) taglibs.next();
				String tagUri = null;
				String tagLoc = null;
				TreeNode child = taglib.findChild("taglib-uri");
				if (child != null)
					tagUri = child.getBody();
				child = taglib.findChild("taglib-location");
				if (child != null)
					tagLoc = child.getBody();

				// Save this location if appropriate
				if (tagLoc == null)
					continue;
				if (uriType(tagLoc) == NOROOT_REL_URI)
					tagLoc = "/WEB-INF/" + tagLoc;
				String tagLoc2 = null;
				if (tagLoc.endsWith(JAR_FILE_SUFFIX)) {
					tagLoc = ctxt.getResource(tagLoc).toString();
					tagLoc2 = "META-INF/taglib.tld";
				}
				mappings.put(tagUri, new String[] { tagLoc, tagLoc2 });
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Throwable t) {
				}
			}
		}
	}

	private void scanWebInf() throws Exception {

		InMemoryWebAppClassLoader webAppClassLoader = (InMemoryWebAppClassLoader) Thread
				.currentThread().getContextClassLoader();

		Map<URL, Resource> resourceMap = webAppClassLoader.getResources();

		for (URL key : resourceMap.keySet()) {
			Resource resource = resourceMap.get(key);
			String resourceName = resource.getOriginalPath();
			if (resource instanceof ArchiveTypeResource
					&& ((ArchiveTypeResource) resource).isRuntimeNeeded()) {
				scanJar((ArchiveTypeResource) resource, true);
			} else if (resource instanceof ArchiveTypeResource
					&& resourceName.startsWith("/WEB-INF")
					&& resourceName.endsWith(".tld")) {
				mappingTldResource(resource);
			}
		}
		/**
		 * SDLoader自体がTagLibを提供する場合、親クラスローダーにさかのぼってチェックをする必要がある。
		 * （/common/libみたいな機能を提供する場合） while (loader != null) { if (loader
		 * instanceof URLClassLoader) { URL[] urls = ((URLClassLoader)
		 * loader).getURLs(); for (int i=0; i<urls.length; i++) { URLConnection
		 * conn = urls[i].openConnection(); if (conn instanceof
		 * JarURLConnection) { if (needScanJar(loader, webappLoader,
		 * ((JarURLConnection) conn).getJarFile().getName())) {
		 * scanJar((JarURLConnection) conn, true); } } else { String urlStr =
		 * urls[i].toString(); if (urlStr.startsWith(FILE_PROTOCOL) &&
		 * urlStr.endsWith(JAR_FILE_SUFFIX) && needScanJar(loader, webappLoader,
		 * urlStr)) { URL jarURL = new URL("jar:" + urlStr + "!/");
		 * scanJar((JarURLConnection) jarURL.openConnection(), true); } } } }
		 *
		 * loader = loader.getParent(); }
		 */
	}

	/**
	 * Scans the given JarURLConnection for TLD files located in META-INF (or a
	 * subdirectory of it), adding an implicit map entry to the taglib map for
	 * any TLD that has a <uri> element.
	 *
	 * @param conn
	 *            The JarURLConnection to the JAR file to scan
	 * @param ignore
	 *            true if any exceptions raised when processing the given JAR
	 *            should be ignored, false otherwise
	 */
	private void scanJar(ArchiveTypeResource archive, boolean ignore)
			throws JasperException {

		try {
			BranchTypeResource metainf = (BranchTypeResource) archive
					.getArchiveResource("/META-INF/");
			if (metainf != null) {
				List<Resource> resources = metainf.getResources();
				scanList(resources);
			}
		} catch (Exception ex) {
			if (!ignore) {
				throw new JasperException(ex);
			}
		}
	}

	private void scanList(List<Resource> resources) throws Exception {
		if (resources == null)
			return;
		for (Resource res : resources) {
			if (res instanceof LeafTypeResource
					&& res.getPath().endsWith(".tld")) {
				mappingTldResource(res);
			} else if (res instanceof DirectoryTypeResource) {
				scanList(((DirectoryTypeResource) res).getResources());
			}
		}
	}

	/*
	 * Add Tld resource information to mapping.
	 */
	private void mappingTldResource(Resource res) throws Exception {

		InputStream stream = res.getResourceAsInputStream();
		String resourcePath = res.getURL().toExternalForm();
		String name = res.getOriginalPath();
		try {
			String uri = getUriFromTld(resourcePath, stream);
			// Add implicit map entry only if its uri is not already
			// present in the map
			if (uri != null && mappings.get(uri) == null) {
				mappings.put(uri, new String[] { resourcePath, name });
			}
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Throwable t) {
					// do nothing
				}
			}
		}
	}

	/*
	 * Returns the value of the uri element of the given TLD, or null if the
	 * given TLD does not contain any such element.
	 */
	private String getUriFromTld(String resourcePath, InputStream in)
			throws JasperException {
		// Parse the tag library descriptor at the specified resource path
		TreeNode tld = new ParserUtils().parseXMLDocument(resourcePath, in);
		TreeNode uri = tld.findChild("uri");
		if (uri != null) {
			String body = uri.getBody();
			if (body != null)
				return body;
		}

		return null;
	}

	// JSP1.2 compatible methods
	private Hashtable<String, TagLibraryInfo> tlds;

	public TagLibraryInfo getTagLibraryInfo(String uri) {
		if (!initialized) {
			try {
				init();
			} catch (JasperException e) {
				throw new RuntimeException(e);
			}
		}
		return (TagLibraryInfo) tlds.get(uri);
	}

	public void addTagLibraryInfo(String uri, TagLibraryInfo tld) {
		if (!initialized) {
			try {
				init();
			} catch (JasperException e) {
				throw new RuntimeException(e);
			}
		}
		tlds.put(uri, tld);
	}
}
