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
package sdloader.javaee.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import sdloader.util.PathUtil;

/**
 * WebApp用クラスローダー
 * 
 * @author c9katayama
 */
public class WebAppClassLoader extends URLClassLoader {

	// WEB-INF以下からはロードしないPrefix
	protected String[] ignoreLoadFromWebInfPackagePrefix = { "java.",
			"com.sun.", "sun." };

	// 先に親ローダーでロードするパッケージPrefix
	protected String[] parentLoadFirstPackagePrefix = { "java.", "javax.",
			"org.apache.el.", "org.apache.jasper.", "org.apache.juli.",
			"com.sun.", "sun.", "org.w3c.", "org.xml.sax.", "org.omg.",
			"org.ietf.jgss", "sdloader." };

	public WebAppClassLoader(URL[] webInfUrls, ClassLoader parent) {
		super(webInfUrls, parent);
	}

	/**
	 * クラスをロードします.
	 * 
	 * <pre>
	 * 1,ロード済みかどうかチェック
	 * 2,SystemClassLoaderからロード
	 * 3,親が先に読むパッケージの場合、親-&gt;子の順にロード
	 * 4,それ以外は子-&gt;親の順にロード
	 * 5,ただし特定のパッケージは子からロードしない。
	 * </pre>
	 */
	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class<?> c = findLoadedClass(name);
		if (c != null) {
			return doResolve(c, resolve);
		}
		return loadAppClass(name, resolve);
	}

	protected Class<?> loadAppClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class<?> c = null;
		if (isParentFirst(name)) {
			c = loadParentFirst(name);
		} else {
			c = loadParentLast(name);
		}
		if (c != null) {
			return doResolve(c, resolve);
		}
		throw new ClassNotFoundException("Class not found.class=[" + name + "]");
	}

	protected Class<?> loadParentFirst(String name) {
		Class<?> c = findFromSystem(name);
		if (c == null) {
			c = findFromParent(name);
		}
		if (c == null) {
			if (isIgnoreLoadFromWebInfPackagePrefix(name) == false) {
				c = findFromWebInf(name);
			}
		}
		return c;
	}

	protected Class<?> loadParentLast(String name) {
		Class<?> c = null;
		if (isIgnoreLoadFromWebInfPackagePrefix(name) == false) {
			c = findFromWebInf(name);
		}
		if (c == null) {
			c = findFromSystem(name);
		}
		if (c == null) {
			c = findFromParent(name);
		}
		return c;
	}

	protected Class<?> findFromWebInf(String name) {
		try {
			return findClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	protected Class<?> findFromSystem(String name) {
		try {
			return findSystemClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	protected Class<?> findFromParent(String name) {
		try {
			return getParent().loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	protected Class<?> doResolve(Class<?> c, boolean resolve) {
		if (resolve) {
			resolveClass(c);
		}
		return c;
	}

	protected boolean isParentFirst(String name) {
		if (parentLoadFirstPackagePrefix != null) {
			for (int i = 0; i < parentLoadFirstPackagePrefix.length; i++) {
				if (name.startsWith(parentLoadFirstPackagePrefix[i])) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean isIgnoreLoadFromWebInfPackagePrefix(String name) {
		if (ignoreLoadFromWebInfPackagePrefix != null) {
			for (int i = 0; i < ignoreLoadFromWebInfPackagePrefix.length; i++) {
				if (name.startsWith(ignoreLoadFromWebInfPackagePrefix[i])) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		final Enumeration<URL> resources = super.getResources(name);
		// duplicate check
		Vector<URL> margeUrlList = new Vector<URL>();
		Set<String> narrowPathSet = new HashSet<String>();
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			String narrowPath = PathUtil.narrow(url.toExternalForm());
			if (narrowPathSet.contains(narrowPath) == false) {
				narrowPathSet.add(narrowPath);
				margeUrlList.add(url);
			}
		}
		return margeUrlList.elements();
	}
}