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
import java.net.URL;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.PathUtil;
import sdloader.util.TextFormatUtil;

/**
 * Webアプリのコンテキストファイルパーサーハンドラー
 * 
 * @author c9Katayama
 * 
 */
public class WebAppContextXmlParserHandler extends DefaultHandler {

	private static SDLoaderLog log = SDLoaderLogFactory
			.getLog(WebAppContextXmlParserHandler.class);

	private String fileName;
	private String webAppDirPath;

	private WebAppContext webAppContext;

	public WebAppContextXmlParserHandler(String fileName, String webAppDirPath) {
		this.fileName = fileName;
		this.webAppDirPath = webAppDirPath;
	}

	public WebAppContext getWebAppContext() {
		return webAppContext;
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals("Context")) {
			processContextTag(attributes);
		}
	}

	protected void processContextTag(Attributes attributes) {
		String contextPath = attributes.getValue("path");
		String docBase = attributes.getValue("docBase");
		if (docBase == null) {
			String message = "docBase attribute not found. file=" + fileName;
			log.error(message);
			throw new RuntimeException(message);
		}
		if (contextPath == null) {
			contextPath = contextPathFromFileName();
		}
		contextPath = TextFormatUtil.formatTextBySystemProperties(contextPath);

		docBase = TextFormatUtil.formatTextBySystemProperties(docBase);
		String[] bases = docBase.replace('\\', '/').split(",");
		URL[] baseURLs = new URL[bases.length];

		for (int i = 0; i < bases.length; i++) {
			String base = bases[i];
			if (base.startsWith(".")) {// 相対パスの場合、webappsまでのパスを追加
				base = webAppDirPath + "/" + base;
			}
			File baseDir = new File(base);
			if (!baseDir.exists()) {
				String message = "docBase not exist. file=" + fileName
						+ " contextPath=" + contextPath + " docBase=" + base;
				log.error(message);
				throw new RuntimeException(message);
			}
			if (baseDir.isFile()) {
				String message = "docBase is File path. file=" + fileName
						+ " contextPath=" + contextPath + " docBase=" + base;
				log.error(message);
				throw new RuntimeException(message);
			}
			baseURLs[i] = PathUtil.file2URL(base);
		}
		log.info("detect webapp context. contextPath=" + contextPath
				+ " docBase=" + docBase);

		webAppContext = new WebAppContext(contextPath, baseURLs);
	}

	protected String contextPathFromFileName() {
		return "/" + fileName.substring(0, fileName.length() - ".xml".length());
	}
}
