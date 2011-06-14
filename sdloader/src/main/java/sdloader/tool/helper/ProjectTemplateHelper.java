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
package sdloader.tool.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sdloader.util.IOUtil;
import sdloader.util.ResourceUtil;

/**
 * プロジェクトテンプレート作成用Helper
 * 
 * @author c9katayama
 */
public class ProjectTemplateHelper {

	public static void execute(String webContentDir, InputStream webXml,
			InputStream indexHtml, InputStream mainJavaTemplate) {
		try {
			File baseDir = new File(".");
			createProjectTemplate(baseDir, webContentDir, webXml, indexHtml);
			File classPathFile = getEclipseClassPathFile(baseDir);
			if (classPathFile != null) {
				modifyEclipseClassPath(baseDir, classPathFile, webContentDir);
			}
			createMainClass(baseDir, mainJavaTemplate);
			System.out.println("Create success.Reflesh the project.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void createProjectTemplate(File baseDir,
			String webContentName, InputStream webXmlStream,
			InputStream indexHtmlStream) throws IOException {

		File webContent;
		if (webContentName == null) {
			webContent = baseDir;
		} else {
			webContent = new File(baseDir, webContentName);
			mkdir(webContentName, webContent);
		}
		File indexHtml = new File(webContent, "index.html");
		if (!indexHtml.exists()) {
			indexHtml.createNewFile();
			FileOutputStream fout = new FileOutputStream(indexHtml);
			ResourceUtil.copyStream(indexHtmlStream, fout);
			fout.flush();
			fout.close();
			System.out.println("Create index.html");
		}

		File webInf = new File(webContent, "WEB-INF");
		mkdir("WEB-INF", webInf);

		File lib = new File(webInf, "lib");
		mkdir("WEB-INF/lib", lib);

		File classes = new File(webInf, "classes");
		mkdir("WEB-INF/classes", classes);

		File webxml = new File(webInf, "web.xml");
		if (!webxml.exists()) {
			webxml.createNewFile();
			FileOutputStream fout = new FileOutputStream(webxml);
			ResourceUtil.copyStream(webXmlStream, fout);
			fout.flush();
			fout.close();
			System.out.println("Create WEB-INF/web.xml");
		}
	}

	private static void mkdir(String name, File dir) throws IOException {
		if (!dir.exists()) {
			if (!dir.mkdir()) {
				throw new IOException(name + " create fail!");
			}
		}
	}

	public static File getEclipseClassPathFile(File projectDir) {
		File classPathFile = new File(projectDir, ".classpath");
		return (classPathFile.exists()) ? classPathFile : null;
	}

	public static boolean modifyEclipseClassPath(File projectDir,
			File classPathFile, String WebContentPath) {
		// eclipse
		try {
			String classesPath = "";
			if (WebContentPath != null) {
				classesPath += WebContentPath + "/";
			}
			classesPath += "WEB-INF/classes";

			Document classPathDocument = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().parse(classPathFile);
			NodeList entryList = classPathDocument.getFirstChild()
					.getChildNodes();
			for (int i = 0; i < entryList.getLength(); i++) {
				Node node = entryList.item(i);
				if (node instanceof Element) {
					Node kindNode = node.getAttributes().getNamedItem("kind");
					String kind = kindNode.getNodeValue();
					if ("output".equals(kind)) {
						Node pathNode = node.getAttributes().getNamedItem(
								"path");
						String oldPath = pathNode.getNodeValue();
						if (!oldPath.equals(classesPath)) {
							pathNode.setNodeValue(classesPath);
							DOMSource source = new DOMSource(classPathDocument);
							FileOutputStream fout = new FileOutputStream(
									classPathFile);
							StreamResult result = new StreamResult(fout);
							TransformerFactory.newInstance().newTransformer()
									.transform(source, result);
							IOUtil.forceRemoveDirectory(new File(projectDir,
									oldPath));
							System.out
									.println("Class file output folder modified.");
							return true;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static void createMainClass(File projectDir,
			InputStream mainJavaTemplateStream) throws IOException {
		File srcDir = new File(projectDir, "src");
		if (srcDir.exists()) {
			File mainFile = new File(srcDir, "SDLoaderStartMain.java");
			if (!mainFile.exists()) {
				mainFile.createNewFile();
				FileOutputStream fout = new FileOutputStream(mainFile);
				ResourceUtil.copyStream(mainJavaTemplateStream, fout);
				fout.flush();
				fout.close();
				System.out.println("Create SDLoaderStartMain.java file.");
			}
		}
	}
}
