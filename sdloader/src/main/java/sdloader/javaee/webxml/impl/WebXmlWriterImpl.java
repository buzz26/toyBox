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
package sdloader.javaee.webxml.impl;

import java.io.IOException;
import java.io.Writer;

import sdloader.exception.IORuntimeException;
import sdloader.javaee.webxml.WebXml;
import sdloader.javaee.webxml.WebXmlVisitor;
import sdloader.javaee.webxml.WebXmlWriter;
import sdloader.javaee.webxml.WebXmlWritingContext;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;

/**
 * 
 * @author shot
 * 
 */
public class WebXmlWriterImpl implements WebXmlWriter {

	protected WebXmlVisitor visitor;

	protected Writer writer;

	protected static SDLoaderLog LOG = SDLoaderLogFactory
			.getLog(WebXmlWriterImpl.class);

	public WebXmlWriterImpl(WebXmlVisitor visitor, Writer writer) {
		this.visitor = visitor;
		this.writer = writer;
	}

	public void write(WebXml webXml) {
		if (webXml == null) {
			throw new NullPointerException("web xml");
		}
		visitor.visit(webXml.getWebApp());
		final WebXmlWritingContext context = visitor.getContext();
		try {
			writer.write(context.getXml());
		} catch (IOException e) {
			throw new IORuntimeException(e);
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				LOG.debug(e.getMessage());
			}
			context.clear();
		}
	}
}
