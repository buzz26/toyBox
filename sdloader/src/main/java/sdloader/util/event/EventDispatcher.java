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
package sdloader.util.event;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import sdloader.util.CollectionsUtil;

/**
 * イベントディスパッチャー
 *
 * @author AKatayama
 * @author shot
 *
 * @param <E>
 *            Event
 * @param <L>
 *            ListenerTest
 */
public class EventDispatcher<L, E extends Event> {

	private String invokeMethodName;
	private Map<String, List<L>> listenerMap = CollectionsUtil.newHashMap();

	/**
	 * イベント発生時に呼び出すメソッド名を引数に、EventDispatcherを 構築します。
	 *
	 * @param invokeMethodName
	 */
	public EventDispatcher(String invokeMethodName) {
		this.invokeMethodName = invokeMethodName;
	}

	/**
	 * リスナーインターフェースのクラスを引数に、EventDispatcherを 構築します。
	 * リスナーインターフェースが1つのメソッド宣言を持つ場合、そのメソッドを呼び出しメソッド として採用します。 それ以外の場合はエラーが発生します。
	 *
	 * @param listenerClass
	 */
	public EventDispatcher(Class<?> listenerClass) {
		detectInvokeMethod(listenerClass);
	}

	public void addEventListener(String type, L listener) {
		List<L> listenerList = listenerMap.get(type);
		if (listenerList == null) {
			listenerList = CollectionsUtil.newArrayList();
			listenerMap.put(type, listenerList);
		}
		listenerList.add(listener);
	}

	public void removeEventListener(String type, L listener) {
		List<L> listenerList = listenerMap.get(type);
		if (listenerList != null) {
			// マッチする全要素削除
			while (listenerList.remove(listener)) {
			}
		}
	}

	/**
	 * 引数のタイプに対するリスナーを全てクリアします。
	 *
	 * @param type
	 */
	public void clear(String type) {
		listenerMap.remove(type);
	}

	/**
	 * 全てのリスナーをクリアします。
	 */
	public void clearAll() {
		listenerMap.clear();
	}

	public void dispatchEvent(E event) {
		List<L> listenerList = listenerMap.get(event.getType());
		if (listenerList != null) {
			for (L listener : listenerList) {
				invoke(listener, event);
			}
		}
	}

	protected void detectInvokeMethod(Class<?> listener) {
		Method[] methods = listener.getMethods();
		if (methods.length != 1) {
			throw new RuntimeException("invoke method detect fail.");
		}
		invokeMethodName = methods[0].getName();
	}

	protected void invoke(L listener, E event) {
		try {
			Method method = listener.getClass().getDeclaredMethod(
					invokeMethodName, new Class[] { event.getClass() });
			if (method == null) {
				throw new NoSuchMethodError(invokeMethodName);
			}
			method.setAccessible(true);
			method.invoke(listener, new Object[] { event });
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}