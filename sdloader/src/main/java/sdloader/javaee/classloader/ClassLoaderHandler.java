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

public interface ClassLoaderHandler {

	/**
	 * クラスロード時に呼ばれます.
	 * 
	 * <pre>
	 * nullを返すと、通常のWebAppClassLoaderの処理が続行します。
	 * Classを返すと、そのクラスをロードします。
	 * ClassNotFoundExceptionを投げると、クラスがないこととして処理を行います。
	 * 
	 * たとえば特定のクラスがクラスパス上にあり、そのクラスをWebアプリに認識させたくない場合、
	 * このハンドラを使い、ClassNotFoundExceptionを投げます。
	 * </pre>
	 * 
	 * @param className
	 * @param resolve
	 * @return
	 * @throws ClassNotFoundException
	 */
	Class<?> handleLoadClass(String className, boolean resolve)
			throws ClassNotFoundException;

	/**
	 * Class.getResouce()時に呼ばれます.
	 * 
	 * <pre>
	 * nullを返すと、通常の処理を続行します。
	 * 空のListを返すと、そのリソースがないものとして処理を行います。
	 * 
	 * たとえばlog4j.xmlというリソースに対して、log4jtest.xmlのファイルパスを返すことで、
	 * テスト時だけテスト用のファイルを使用したり出来ます。
	 * </pre>
	 * 
	 * @param resounceName
	 * @return
	 * @throws IOException
	 */
	List<URL> handleResources(String resourceName) throws IOException;
}
