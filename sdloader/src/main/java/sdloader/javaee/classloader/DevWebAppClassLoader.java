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
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import sdloader.exception.IORuntimeException;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;

/**
 * 
 * 開発用のクラスローダー
 * 
 * <pre>
 * ロードしたクラスのログ出力と、ClassLoaderHandlerの処理を行います。
 * </pre>
 * 
 * @author c9katayama
 * 
 */
public class DevWebAppClassLoader extends WebAppClassLoader {

	protected SDLoaderLog log = SDLoaderLogFactory
			.getLog(DevWebAppClassLoader.class);

	protected boolean debugEnable = log.isDebugEnabled();

	protected ClassLoaderHandler classLoaderHandler = new ClassLoaderHandlerAdapter();

	public DevWebAppClassLoader(URL[] webInfUrls, ClassLoader parent) {
		super(webInfUrls, parent);
	}

	public DevWebAppClassLoader(URL[] webInfUrls, ClassLoader parent,
			ClassLoaderHandler classLoaderHandler) {
		super(webInfUrls, parent);
		setClassLoaderHandler(classLoaderHandler);
	}

	public void setClassLoaderHandler(ClassLoaderHandler classLoaderHandler) {
		if (classLoaderHandler != null) {
			this.classLoaderHandler = classLoaderHandler;
		}
	}

	protected Class<?> loadAppClass(String name, boolean resolve)
			throws ClassNotFoundException {
		Class<?> c = classLoaderHandler.handleLoadClass(name, resolve);
		if (c != null) {
			if (debugEnable) {
				log.debug("Class load from class loader handler. class=["
						+ name + "]");
			}
			return doResolve(c, resolve);
		}
		return super.loadAppClass(name, resolve);
	}

	@Override
	protected Class<?> findFromWebInf(String name) {
		Class<?> c = super.findFromWebInf(name);
		if (c != null) {
			if (debugEnable) {
				log.debug("Class load from app[" + this.hashCode()
						+ "]. class=[" + name + "]");
			}
		}
		return c;
	}

	@Override
	protected Class<?> findFromSystem(String name) {
		Class<?> c = super.findFromSystem(name);
		if (c != null) {
			if (debugEnable) {
				log.debug("Class load from system. class=[" + name + "]");
			}
		}
		return c;
	}

	@Override
	protected Class<?> findFromParent(String name) {
		Class<?> c = super.findFromParent(name);
		if (c != null) {
			if (debugEnable) {
				log.debug("Class load from parent. class=[" + name + "]");
			}
		}
		return c;
	}

	@Override
	public URL getResource(String name) {
		try {
			Enumeration<URL> resource = getResources(name);
			if (resource.hasMoreElements()) {
				return resource.nextElement();
			} else {
				return super.getResource(name);
			}
		} catch (IOException ioe) {
			throw new IORuntimeException(ioe);
		}
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		List<URL> handleResourecs = classLoaderHandler.handleResources(name);
		if (handleResourecs != null) {
			Vector<URL> handleUrlList = new Vector<URL>();
			handleUrlList.addAll(handleResourecs);
			return handleUrlList.elements();
		}
		return super.getResources(name);
	}

}
