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
package sdloader.javaee.impl;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;

import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.ListenerEventDispatcher;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.CollectionsUtil;
import sdloader.util.IteratorEnumeration;

/**
 * HttpSession実装クラス
 * 
 * @author c9katayama
 */
@SuppressWarnings("deprecation")
public class HttpSessionImpl implements HttpSession {

	private static SDLoaderLog log = SDLoaderLogFactory
			.getLog(HttpSessionImpl.class);

	private String id;

	private long creationTime;

	private long lastAccessedTime;

	private int maxInactiveInterval;

	private Map<String, Object> attributeMap = CollectionsUtil.newHashMap();

	private boolean invalidate = false;

	private boolean isNew = true;

	private InternalWebApplication internalWebApplication;

	public HttpSessionImpl(InternalWebApplication webApp, String sessionId) {
		this.id = sessionId;
		this.creationTime = System.currentTimeMillis();
		this.internalWebApplication = webApp;
		dispatchCreateEvent();
	}

	public long getCreationTime() {
		checkInvalidate();
		return creationTime;
	}

	public String getId() {
		checkInvalidate();
		return id;
	}

	public long getLastAccessedTime() {
		checkInvalidate();
		return lastAccessedTime;
	}

	public ServletContext getServletContext() {
		checkInvalidate();
		return internalWebApplication.getServletContext();
	}

	public void setMaxInactiveInterval(int interval) {
		checkInvalidate();
		this.maxInactiveInterval = interval;
	}

	public int getMaxInactiveInterval() {
		checkInvalidate();
		return maxInactiveInterval;
	}

	public HttpSessionContext getSessionContext() {
		throw new RuntimeException("getSessionConetxt not implemented");
	}

	public Object getAttribute(String key) {
		checkInvalidate();
		return attributeMap.get(key);
	}

	public Object getValue(String key) {
		checkInvalidate();
		return getAttribute(key);
	}

	public Enumeration<String> getAttributeNames() {
		checkInvalidate();
		return new IteratorEnumeration<String>(
				attributeMap.keySet().iterator(), true);
	}

	public String[] getValueNames() {
		checkInvalidate();
		return attributeMap.keySet().toArray(new String[] {});
	}

	public void setAttribute(String key, Object value) {
		checkInvalidate();
		if (key == null) {
			throw new IllegalArgumentException("Session attribute key is null.");
		}
		if (value == null) {
			removeAttribute(key);
		} else {
			Object oldValue = attributeMap.get(key);
			if (value == oldValue) {
				return;
			}
			if (value instanceof HttpSessionBindingListener) {
				HttpSessionBindingEvent boundEvent = new HttpSessionBindingEvent(
						this, key, value);
				try {
					((HttpSessionBindingListener) value).valueBound(boundEvent);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}

			this.attributeMap.put(key, value);

			ListenerEventDispatcher dispatcher = internalWebApplication
					.getListenerEventDispatcher();
			if (oldValue == null) {
				HttpSessionBindingEvent event = new HttpSessionBindingEvent(
						this, key, value);
				dispatcher
						.dispatchHttpSessionAttributeListener_attributeAdded(event);
			} else {
				if (oldValue instanceof HttpSessionBindingListener) {
					HttpSessionBindingEvent unBoundEvent = new HttpSessionBindingEvent(
							this, key, oldValue);
					try {
						((HttpSessionBindingListener) oldValue)
								.valueUnbound(unBoundEvent);
					} catch (Throwable t) {
						log.error(t.getMessage(), t);
					}
				}
				HttpSessionBindingEvent event = new HttpSessionBindingEvent(
						this, key, oldValue);
				dispatcher
						.dispatchHttpSessionAttributeListener_attributeReplaced(event);
			}
		}
	}

	public void putValue(String key, Object value) {
		checkInvalidate();
		setAttribute(key, value);
	}

	public void removeAttribute(String key) {
		checkInvalidate();
		Object oldValue = attributeMap.remove(key);
		if (oldValue != null) {
			HttpSessionBindingEvent event = new HttpSessionBindingEvent(this,
					key, oldValue);
			if (oldValue instanceof HttpSessionBindingListener) {
				try {
					((HttpSessionBindingListener) oldValue).valueUnbound(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
			ListenerEventDispatcher dispatcher = internalWebApplication
					.getListenerEventDispatcher();
			dispatcher
					.dispatchHttpSessionAttributeListener_attributeRemoved(event);
		}
	}

	public void removeValue(String key) {
		checkInvalidate();
		removeAttribute(key);
	}

	public void invalidate() {
		if (!invalidate) {
			dispatchDestroyEvent();
			this.invalidate = true;
			internalWebApplication = null;
			attributeMap.clear();
			attributeMap = null;
		}
	}

	public boolean isNew() {
		return isNew;
	}

	// /non interface method
	protected void dispatchCreateEvent() {
		if (internalWebApplication == null) {
			return;
		}
		HttpSessionEvent event = new HttpSessionEvent(this);
		ListenerEventDispatcher dispatcher = internalWebApplication
				.getListenerEventDispatcher();
		dispatcher.dispatchHttpSessionListener_sessionCreated(event);
	}

	protected void dispatchDestroyEvent() {
		if (internalWebApplication == null) {
			return;
		}
		HttpSessionEvent event = new HttpSessionEvent(this);
		ListenerEventDispatcher dispatcher = internalWebApplication
				.getListenerEventDispatcher();
		dispatcher.dispatchHttpSessionListener_sessionDestroyed(event);
	}

	public void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public boolean isInvalidate() {
		return invalidate;
	}

	private void checkInvalidate() {
		if (invalidate) {// 使用不可能
			throw new IllegalStateException("Session invalidated.");
		}
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}
}
