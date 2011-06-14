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

import java.net.MalformedURLException;
import java.net.URL;

public class WarProtocolBuilder {

	public static final String WAR_PROTOCOL = "war:";

	public static final String RESOURCE_SEPARATOR = "!";

	public static URL createArchiveResourceURL(final URL archiveUrl,
			final String resourcePath) {
		assertBothNotNull(archiveUrl, resourcePath);
		final String urlStr = archiveUrl.toExternalForm();
		StringBuilder builder = new StringBuilder();
		builder.append(urlStr);
		builder.append(RESOURCE_SEPARATOR);
		if (!resourcePath.startsWith("/")) {
			builder.append("/");
		}
		builder.append(resourcePath);
		return createURL(builder);
	}

	public static URL innerjarUrl(final URL rootUrl, final String originalPath) {
		assertBothNotNull(rootUrl, originalPath);
		final String urlStr = rootUrl.toExternalForm();
		StringBuilder builder = new StringBuilder();
		builder.append(JarArchiveTypeResourceImpl.PROTOCOL);
		builder.append(urlStr);
		builder.append(RESOURCE_SEPARATOR);
		if (!originalPath.startsWith("/")) {
			builder.append("/");
		}
		builder.append(originalPath);
		return createURL(builder);
	}

	public static URL classPathToUrl(final URL rootUrl,
			final String originalPath) {
		assertBothNotNull(rootUrl, originalPath);
		final String s = originalPath;// originalPath.replace(ClassTypeResource.
		// WEBINF_PREFIX,"");
		final String urlStr = rootUrl.toExternalForm();
		StringBuilder builder = new StringBuilder();
		builder.append(WAR_PROTOCOL);
		builder.append(urlStr);
		builder.append(RESOURCE_SEPARATOR);
		if (!originalPath.startsWith("/")) {
			builder.append("/");
		}
		builder.append(s);
		return createURL(builder);
	}

	public static URL resourcePathToUrl(final URL rootUrl,
			final String originalPath) {
		assertBothNotNull(rootUrl, originalPath);
		final String urlStr = rootUrl.toExternalForm();
		StringBuilder builder = new StringBuilder();
		builder.append(WAR_PROTOCOL);
		builder.append(urlStr);
		builder.append(RESOURCE_SEPARATOR);
		if (!originalPath.startsWith("/")) {
			builder.append("/");
		}
		builder.append(originalPath);
		return createURL(builder);
	}

	private static URL createURL(StringBuilder builder) {
		try {
			return new URL(null, builder.toString());
		} catch (MalformedURLException ignore) {
			return null;
		}
	}

	private static void assertBothNotNull(final URL rootUrl,
			final String originalPath) {
		if (rootUrl == null || originalPath == null) {
			throw new IllegalArgumentException();
		}
	}

}
