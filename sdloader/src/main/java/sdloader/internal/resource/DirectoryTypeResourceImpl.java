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
package sdloader.internal.resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import sdloader.util.CollectionsUtil;

/**
 * 
 * @author c9katayama
 */
public class DirectoryTypeResourceImpl implements DirectoryTypeResource {

	protected static final byte[] ZERO_LENGTH_BYTE = new byte[0];

	protected String path;

	protected String originalPath;

	protected URL url;

	protected List<Resource> childResources;

	public DirectoryTypeResourceImpl(final URL rootUrl, final String path) {
		this.path = path;
		this.originalPath = path;
		this.url = WarProtocolBuilder.createArchiveResourceURL(rootUrl,
				originalPath);
	}

	public void addResource(Resource resource) {
		if (childResources == null) {
			childResources = CollectionsUtil.newArrayList();
		}
		childResources.add(resource);
	}

	public void addResources(List<Resource> resources) {
		if (childResources == null) {
			childResources = CollectionsUtil.newArrayList();
		}
		childResources.addAll(resources);
	}

	public List<Resource> getResources() {
		return childResources;
	}

	public String getOriginalPath() {
		return path;
	}

	public String getPath() {
		return path;
	}

	public byte[] getResourceAsBytes() {
		return ZERO_LENGTH_BYTE;
	}

	public InputStream getResourceAsInputStream() {
		return new ByteArrayInputStream(ZERO_LENGTH_BYTE);
	}

	public URL getURL() {
		return url;
	}
}
