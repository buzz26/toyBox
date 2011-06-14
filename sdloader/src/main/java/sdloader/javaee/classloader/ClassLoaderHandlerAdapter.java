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
import java.util.List;

/**
 * WebAppClassLoaderに介入するためのハンドラーアダプター
 * 
 * @author c9katayama
 * 
 */
public class ClassLoaderHandlerAdapter implements ClassLoaderHandler {

	/*
	 * (non-Javadoc)
	 * 
	 * @see sdloader.javaee.classloader.ClassLoaderHandler#handleLoadClass(java.lang.String,
	 *      boolean)
	 */
	public Class<?> handleLoadClass(String className, boolean resolve)
			throws ClassNotFoundException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sdloader.javaee.classloader.ClassLoaderHandler#handleResources(java.lang.String)
	 */
	public List<URL> handleResources(String resourceName) throws IOException {
		return null;
	}

}
