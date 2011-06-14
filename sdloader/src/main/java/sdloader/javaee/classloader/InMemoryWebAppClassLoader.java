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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sdloader.internal.resource.ArchiveTypeResource;
import sdloader.internal.resource.BranchTypeResource;
import sdloader.internal.resource.Resource;

/**
 * load classes and resources on memory.
 * 
 * @author shot
 * @author c9katayama
 */
public class InMemoryWebAppClassLoader extends WebAppClassLoader {

	private Map<URL, Resource> resources;

	public InMemoryWebAppClassLoader(Map<URL, Resource> resources,
			URL[] classPathURLs, ClassLoader parent) {
		super(classPathURLs, parent);
		this.resources = resources;
	}

	@Override
	public URL getResource(String name) {
		Resource res = findClassPathResource(name);
		return (res != null) ? res.getURL() : null;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		List<Resource> resourceList = findClassPathResources(name, false);
		final Iterator<Resource> itr = resourceList.iterator();
		return new Enumeration<URL>() {
			public boolean hasMoreElements() {
				return itr.hasNext();
			}

			public URL nextElement() {
				Resource res = itr.next();
				return res.getURL();
			}
		};
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// メモリ上のリソースからロード
		String classResourceName = name.replace(".", "/") + ".class";
		Resource classResource = findClassPathResource(classResourceName);
		if (classResource != null) {
			byte[] bytes = classResource.getResourceAsBytes();
			Class<?> c = defineClass(name, bytes, 0, bytes.length);
			return c;
		} else {
			throw new ClassNotFoundException(name);
		}
	}

	public Map<URL, Resource> getResources() {
		return resources;
	}

	protected Resource findClassPathResource(String name) {
		List<Resource> resourceList = findClassPathResources(name, true);
		return resourceList.size() == 0 ? null : resourceList.get(0);
	}

	protected List<Resource> findClassPathResources(String name,
			boolean firstOnly) {

		List<Resource> resourceList = new ArrayList<Resource>();
		URL[] classPathURLs = getURLs();
		for (int i = 0; i < classPathURLs.length; i++) {
			final URL classPathBase = classPathURLs[i];
			final Resource classPathBaseResource = resources.get(classPathBase);
			if (classPathBaseResource instanceof BranchTypeResource) {
				try {
					URL resourceUrl = new URL(classPathBase.toExternalForm()
							+ name);
					final Resource classPathResource = resources
							.get(resourceUrl);
					if (classPathResource != null) {
						resourceList.add(classPathResource);
					}
				} catch (Exception e) {
					// ignore
				}
			} else if (classPathBaseResource instanceof ArchiveTypeResource) {
				final Resource classPathResource = ((ArchiveTypeResource) classPathBaseResource)
						.getArchiveResource(name);
				if (classPathResource != null) {
					resourceList.add(classPathResource);
				}
			}
		}
		return resourceList;
	}
}
