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
package sdloader.javaee.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sdloader.constants.HttpConstants;
import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.impl.ServletContextImpl;
import sdloader.javaee.webxml.ServletMappingTag;
import sdloader.javaee.webxml.WelcomeFileListTag;
import sdloader.util.PathUtil;
import sdloader.util.ResourceUtil;
import sdloader.util.WebUtil;

/**
 * ファイル出力サーブレット リクエストパスからファイルを検索し、返します。
 * 
 * @author c9katayama
 */
public class FileSavingServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String PARAM_DOC_ROOT = "docRootPath";
	public static final String PARAM_IGNORE_LAST_MODIFIED = "ignoreLastModified";

	/**
	 * ドキュメントルート（絶対パス）
	 */
	protected URL[] docRootPath;

	protected WelcomeFileListTag welcomeFileListTag;

	protected InternalWebApplication webApp;

	protected boolean ignoreLastModified;

	public FileSavingServlet() {
		super();
	}

	public void init() throws ServletException {
		String docRootStr = getInitParameter(PARAM_DOC_ROOT);
		if (docRootStr == null) {
			throw new ServletException("InitParameter [docRootPath] not found.");
		}
		String[] paths = docRootStr.split(",");
		this.docRootPath = new URL[paths.length];
		for (int i = 0; i < paths.length; i++) {
			docRootPath[i] = ResourceUtil.createURL(paths[i]);
		}

		ServletContextImpl servletContext = (ServletContextImpl) getServletContext();
		webApp = servletContext.getWebApplication();
		welcomeFileListTag = webApp.getWebXml().getWebApp()
				.getWelcomeFileList();

		String ignoreLastModifiedParam = getInitParameter(PARAM_IGNORE_LAST_MODIFIED);
		if (ignoreLastModifiedParam != null) {
			ignoreLastModified = Boolean.parseBoolean(ignoreLastModifiedParam);
		}
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doIt(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		doIt(req, res);
	}

	protected void doIt(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		String uri = req.getPathInfo();

		if (uri == null) {
			processNotFound(res);
			return;
		}

		if (uri.startsWith("/WEB-INF/") || uri.endsWith("/WEB-INF")) {
			processNotFound(res);
			return;
		}
		File fileOrDir = null;
		for (int i = 0; i < docRootPath.length; i++) {
			File testFile = PathUtil.url2File(this.docRootPath[i]
					.toExternalForm()
					+ uri);
			if (testFile.exists()) {
				fileOrDir = testFile;
				break;
			}
		}
		if (fileOrDir == null) {
			processNotFound(res);
			return;
		}
		if (fileOrDir.isFile()) {
			File file = fileOrDir;
			processOutputFile(file, req, res);
			return;
		} else {
			if (welcomeFileListTag != null) {
				File dir = fileOrDir;
				processWelcomeFile(dir.toURI().toURL(), req, res);
				return;
			} else {
				processNotFound(res);
				return;
			}
		}
	}

	/**
	 * welcomeファイルの処理 welcomeファイルリストのパスに対して、パターンが完全一致するサーブレット
	 * かファイルを検索し、見つかった場合forwardします。
	 * 
	 * @param dir
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void processWelcomeFile(final URL dir,
			final HttpServletRequest req, final HttpServletResponse res)
			throws ServletException, IOException {
		String basePath = req.getPathInfo();
		basePath = PathUtil.appendEndSlashIfNeed(basePath);
		ServletContextImpl context = (ServletContextImpl) getServletContext();
		InternalWebApplication webapp = context.getWebApplication();
		List<String> welcomeFileList = welcomeFileListTag.getWelcomeFileList();
		List<ServletMappingTag> servletMappingList = webapp.getWebXml()
				.getWebApp().getServletMapping();

		for (String welcomefileName : welcomeFileList) {
			String path = basePath + welcomefileName;
			// find servlet mapping
			for (ServletMappingTag mappingTag : servletMappingList) {
				int matchType = WebUtil.matchPattern(
						mappingTag.getUrlPattern(), path);
				if (matchType == WebUtil.PATTERN_EXACT_MATCH) {// 完全
					context.getRequestDispatcher(path).forward(req, res);
					return;
				}
			}
			// find file
			URL welcomeFile = ResourceUtil.createURL(dir, welcomefileName);
			if (ResourceUtil.isResourceExist(welcomeFile)) {
				context.getRequestDispatcher(path).forward(req, res);
				return;
			}
		}
		processNotFound(res);
		return;
	}

	/**
	 * ファイル出力
	 * 
	 * @param file
	 * @param req
	 * @param res
	 * @throws IOException
	 */
	protected void processOutputFile(File file, HttpServletRequest req,
			HttpServletResponse res) throws IOException {
		if (!ignoreLastModified) {
			Date lastModifyDate = new Date(file.lastModified());
			res.setHeader(HttpConstants.LASTMODIFIED, WebUtil
					.formatHeaderDate(lastModifyDate));
			String ifModified = req.getHeader(HttpConstants.IFMODIFIEDSINCE);
			// 変更したかどうか
			if (!isModifiedSince(lastModifyDate, ifModified)) {
				res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
		}

		FileInputStream fin = new FileInputStream(file);
		ServletOutputStream sout = res.getOutputStream();
		try {
			long size = ResourceUtil.copyStream(fin, sout);
			setContentType(res, file.getName());
			if (size <= Integer.MAX_VALUE) {
				res.setContentLength((int) size);
			}
			res.setStatus(HttpServletResponse.SC_OK);
		} finally {
			fin.close();
			sout.flush();
		}
	}

	protected void processNotFound(HttpServletResponse res) throws IOException {
		WebUtil.writeNotFoundPage(res);
	}

	/**
	 * ファイルを変更したかどうか
	 * 
	 * @param fileLastModify
	 * @param ifModifiedSince
	 * @return 変更していた場合true,していない場合false
	 */
	protected boolean isModifiedSince(Date fileLastModify,
			String ifModifiedSince) {
		if (ifModifiedSince == null)
			return true;
		try {
			long ifModifiedTime = WebUtil.parseHeaderDate(ifModifiedSince
					.trim());
			long lastModifyTime = fileLastModify.getTime();
			if (lastModifyTime <= ifModifiedTime) {
				return false;
			} else {
				return true;
			}
		} catch (NumberFormatException ne) {
			return true;
		} catch (ParseException e) {
			return true;
		}
	}

	protected void setContentType(HttpServletResponse res, String path) {
		int dotIndex = path.lastIndexOf(".");
		if (dotIndex >= 0) {
			ServletContext sc = getServletContext();
			try {
				String ext = path.substring(dotIndex + 1);
				String type = sc.getMimeType(ext);
				if (type == null) {
					type = sc.getMimeType(ext.toLowerCase());
				}
				if (type == null) {
					type = sc.getMimeType(ext.toUpperCase());
				}
				if (type != null) {
					res.setContentType(type);
					return;
				}
			} catch (Exception e) {
				// ignore
			}
		}
		// default
		res.setContentType("text/html");
	}
}
