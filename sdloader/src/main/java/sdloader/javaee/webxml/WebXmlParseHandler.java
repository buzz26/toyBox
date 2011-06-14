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
package sdloader.javaee.webxml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sdloader.util.Assertion;
import sdloader.util.ClassUtil;
import sdloader.util.CollectionsUtil;

/**
 * web-xmlパース用ハンドラ タグをパースし、xmlをインスタンス化します。 タグは、次のように解析します。
 * ・タグ名を読み、tag2classMapからクラス名を取得 ・クラス名がある場合、そのクラスをインスタンス化しMapに入れ、 解析を続行する。
 * タグが閉じたところで、直前にインスタンス化したクラスのプロパティ として、set+タグ名とadd+タグ名でプロパティのセットを試みる
 * ・クラス名がない場合、直前にインスタンス化したクラスのプロパティとみなし、 set+タグ名とadd+タグ名で属性のセットを試みる
 * ・getRootObject()で、最初にインスタンス化したクラスを返す。
 * 
 * @author c9katayama
 * @author shot
 */
public class WebXmlParseHandler extends DefaultHandler {

	private Map<String, Class<? extends WebXmlTagElement>> tag2classMap = CollectionsUtil
			.newHashMap();

	{
		tag2classMap.put("web-app", WebAppTag.class);
		tag2classMap.put("context-param", ContextParamTag.class);
		tag2classMap.put("filter", FilterTag.class);
		tag2classMap.put("filter-mapping", FilterMappingTag.class);
		tag2classMap.put("listener", ListenerTag.class);
		tag2classMap.put("servlet", ServletTag.class);
		tag2classMap.put("servlet-mapping", ServletMappingTag.class);
		tag2classMap.put("init-param", InitParamTag.class);
		tag2classMap.put("welcome-file-list", WelcomeFileListTag.class);
		tag2classMap.put("error-page", ErrorPageTag.class);
	}

	private Map<String, URL> resolveMap = CollectionsUtil.newHashMap();

	private Map<String, WebXmlTagElement> tagInstanceMap = CollectionsUtil
			.newHashMap();

	private Stack<String> tagNameStack = CollectionsUtil.newStack();

	private String characters;

	private Object rootObject;

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		tagNameStack.push(Assertion.notNull(qName));
		characters = null;
		Class<? extends WebXmlTagElement> tagClass = tag2classMap.get(qName);
		if (tagClass != null) {
			WebXmlTagElement tag = ClassUtil.newInstance(tagClass);
			tagInstanceMap.put(qName, tag);
			if (rootObject == null)
				rootObject = tag;
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		char[] c = new char[length];
		System.arraycopy(ch, start, c, 0, length);
		String value = new String(c).trim();
		if (characters == null) {
			characters = value;
		} else {
			characters += value;
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		String valueTagName = (String) tagNameStack.pop();
		if (tagNameStack.isEmpty()) {
			return;
		}
		Object o = tagInstanceMap.remove(valueTagName);
		if (o == null) {
			o = characters;
		}
		if (o != null) {
			Object target = tagInstanceMap.get(tagNameStack.peek());
			if (target != null) {
				String methodName = "set" + toCamelCase(valueTagName);
				if (!invoke(methodName, target, o)) {
					methodName = "add" + toCamelCase(valueTagName);
					invoke(methodName, target, o);
				}
			}
		}
	}

	public Object getRootObject() {
		return rootObject;
	}

	private String toCamelCase(String tagName) {
		char[] chars = tagName.toCharArray();
		String camelCase = "";
		boolean toUpper = false;
		for (int i = 0; i < chars.length; i++) {
			String c = String.valueOf(chars[i]);
			if (i == 0 || toUpper) {
				camelCase += c.toUpperCase();
				toUpper = false;
			} else {
				if (c.equals("-"))
					toUpper = true;
				else
					camelCase += c;
			}
		}
		return camelCase;
	}

	private boolean invoke(String methodName, Object target, Object o) {
		Method setter = ClassUtil.getMethodNoException(target.getClass(),
				methodName, new Class[] { o.getClass() });
		if (setter != null) {
			ClassUtil.invoke(target, setter, new Object[] { o });
			return true;
		}

		Method[] methods = target.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (method.getName().equals(methodName)
					&& method.getParameterTypes().length == 1) {
				Class<?> paramType = method.getParameterTypes()[0];
				if (paramType == Integer.class || paramType == Integer.TYPE) {
					o = Integer.parseInt(o.toString());
				} else if (paramType == Class.class) {
					o = ClassUtil.forName(o.toString());
				} else {
					continue;
				}
				ClassUtil.invoke(target, method, new Object[] { o });
				return true;
			}
		}
		return false;
	}

	public void register(final String id, final URL resourceUrl) {
		resolveMap.put(id, resourceUrl);
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		URL resourceUrl = (URL) resolveMap.get(publicId);
		if (resourceUrl == null) {
			resourceUrl = (URL) resolveMap.get(systemId);
		}
		if (resourceUrl != null) {
			InputStream is = resourceUrl.openStream();
			return new InputSource(is);
		} else {
			return super.resolveEntity(publicId, systemId);
		}
	}
}
