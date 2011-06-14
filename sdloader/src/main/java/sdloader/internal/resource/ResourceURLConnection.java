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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

/**
 * URLConnection for Resource.
 * 
 * @author c9katayama
 */
public class ResourceURLConnection extends URLConnection {

	private Resource resource;

	public ResourceURLConnection(Resource resource) {
		super(resource != null ? resource.getURL() : null);
		this.resource = resource;
	}

	@Override
	public void connect() throws IOException {
		checkNonNullResource();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		checkNonNullResource();
		return resource.getResourceAsInputStream();
	}

	public Resource getResource() {
		return resource;
	}

	private void checkNonNullResource() throws IOException {
		if (resource == null)
			throw new IOException("Resource is null.");
	}
}
