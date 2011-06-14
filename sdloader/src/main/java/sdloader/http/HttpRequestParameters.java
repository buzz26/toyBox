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
package sdloader.http;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import sdloader.SDLoader;
import sdloader.constants.HttpConstants;
import sdloader.internal.SDLoaderConfig;
import sdloader.util.CollectionsUtil;
import sdloader.util.ResourceUtil;

/**
 * HTTPパラメータ
 * 
 * @author c9katayama
 * @author shot
 */
public class HttpRequestParameters {

	private ParameterContext parameterContext;

	// パラメータをデコードする際のデフォルトエンコーディング
	private String defaultEncoding;

	// body部分のエンコーディング
	private String bodyEncoding;

	// GETのQueryに対して、bodyEncodingを使用するかどうか
	private boolean useBodyEncodingForURI;

	private HttpHeader header;
	private HttpBody body;

	public HttpRequestParameters(HttpHeader header, HttpBody body) {
		this.header = header;
		this.body = body;

		// TODO RequestScopeからのパラメータ取得はどこかにまとめるべき
		String encode = "ISO-8859-1";
		boolean useBodyEncode = true;
		SDLoader loader = ProcessScopeContext.getContext().getSDLoader();
		if (loader != null) {
			SDLoaderConfig config = loader.getSDLoaderConfig();
			encode = config.getConfigString(
					HttpRequest.KEY_REQUEST_DEFAULT_ENCODE, encode);
			useBodyEncode = config.getConfigBoolean(
					HttpRequest.KEY_REQUEST_USE_BODY_ENCODEING_FOR_URI,
					useBodyEncode);

		}
		setDefaultEncoding(encode);
		setUseBodyEncodingForURI(useBodyEncode);
	}

	protected void initIfNeed() {
		if (parameterContext == null) {
			initParameters();
		}
	}

	/**
	 * HttpRequestParameters#getParameterが呼ばれた初回に呼ばれます。
	 */
	private void initParameters() {
		parameterContext = new ParameterContext();
		if (header.getQueryString() != null) {

			parameterContext.parseRequestQuery(header.getQueryString(),
					getQueryEncoding());
		}
		if (body.getSize() > 0
				&& header.getMethod().equalsIgnoreCase(HttpConstants.POST)) {
			String contType = header.getHeaderValue(HttpConstants.CONTENTTYPE);
			if (contType != null) {
				contType = contType.toLowerCase();
				if (contType.indexOf(HttpConstants.WWW_FORM_URLENCODE) != -1) {
					try {
						byte[] postData = ResourceUtil.getBytes(body
								.getInputStream());
						String bodyPartQueryString = new String(postData);
						parameterContext.parseRequestQuery(bodyPartQueryString,
								bodyEncoding);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	public String getQueryEncoding() {
		return (isUseBodyEncodingForURI()) ? bodyEncoding : defaultEncoding;
	}

	public String getParamter(String key) {
		initIfNeed();
		return parameterContext.getParamter(key);
	}

	public String[] getParameterValues(String key) {
		initIfNeed();
		return parameterContext.getParameterValues(key);
	}

	public Iterator<String> getParameterNames() {
		initIfNeed();
		return parameterContext.getParameterNames();
	}

	public Map<String, String[]> getParameterMap() {
		initIfNeed();
		return parameterContext.getParameterMap();
	}

	/**
	 * デフォルトのエンコーディングをセットします。 bodyEncodingもこのエンコーディングに変更されます。
	 * 
	 * @param defaultEncoding
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
		setBodyEncoding(defaultEncoding);
	}

	public String getDefaultEncoding() {
		return defaultEncoding;
	}

	/**
	 * <pre>
	 * body部分のエンコーディングをセットします。 
	 * useBodyEncodingForURIがtrueの場合は、GETパラメータにもこのエンコードが適用されます。
	 * </pre>
	 * 
	 * @param bodyEncoding
	 */
	public void setBodyEncoding(String bodyEncoding) {
		this.bodyEncoding = bodyEncoding;
		parameterContext = null;
	}

	public String getBodyEncoding() {
		return bodyEncoding;
	}

	public void setUseBodyEncodingForURI(boolean useBodyEncodingForURI) {
		this.useBodyEncodingForURI = useBodyEncodingForURI;
		parameterContext = null;
	}

	public boolean isUseBodyEncodingForURI() {
		return useBodyEncodingForURI;
	}

	public static class ParameterContext {

		private Map<String, String[]> paramMap = CollectionsUtil.newHashMap();

		private List<String> paramNameList = CollectionsUtil.newArrayList();

		public void addAll(Map<String, String[]> params) {
			for (Entry<String, String[]> entry : params.entrySet()) {
				String key = entry.getKey();
				String[] values = entry.getValue();
				if (values != null) {
					for (String value : values) {
						addParameter(key, value);
					}
				}
			}
		}

		public String getParamter(String key) {
			String[] paramList = paramMap.get(key);
			if (paramList == null) {
				return null;
			}
			String param = paramList[0];
			return param;
		}

		public String[] getParameterValues(String key) {
			String[] params = paramMap.get(key);
			if (params == null) {
				return null;
			}
			return params;
		}

		public Iterator<String> getParameterNames() {
			return paramNameList.iterator();
		}

		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> newMap = CollectionsUtil.newHashMap();
			newMap.putAll(paramMap);
			return newMap;
		}

		public void parseRequestQuery(String query, String encode) {
			StringTokenizer paramToken = new StringTokenizer(query, "&", false);

			while (paramToken.hasMoreElements()) {
				String param = paramToken.nextToken();
				StringTokenizer token = new StringTokenizer(param, "=", false);
				String key = token.nextToken();
				String value = "";
				if (token.hasMoreTokens()) {
					value = token.nextToken();
				}
				try {
					key = decode(key, encode);
					value = decode(value, encode);
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException(e);
				}
				addParameter(key, value);
			}
		}

		private String decode(String value, String encode)
				throws UnsupportedEncodingException {
			// ISO-8859-1を使用すると、ダイレクトに入力バイトが取れる。
			// バイトを取ってエンコード
			value = URLDecoder.decode(value, "ISO-8859-1");
			value = new String(value.getBytes("ISO-8859-1"), encode);
			return value;
		}

		private void addParameter(String key, String value) {
			if (value == null) {
				return;
			}
			String[] params = paramMap.get(key);
			if (params == null) {
				params = new String[] { value };
				paramMap.put(key, params);
				paramNameList.add(key);
			} else {
				String[] newParams = new String[params.length + 1];
				System.arraycopy(params, 0, newParams, 0, params.length);
				newParams[newParams.length - 1] = value;
				paramMap.put(key, newParams);
			}
		}
	}

}
