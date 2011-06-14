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
package sdloader.javaee;

import java.util.List;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.CollectionsUtil;

/**
 * Dispatch listener event.
 * 
 * @author c9katayama
 */
public class ListenerEventDispatcher {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(ListenerEventDispatcher.class);

	private List<ServletContextListener> servletContextListenerList;
	private List<ServletContextAttributeListener> servletContextAttributeListenerList;
	private List<HttpSessionActivationListener> httpSessionActivationListenerList;
	private List<HttpSessionAttributeListener> httpSessionAttributeListenerList;
	private List<HttpSessionListener> httpSessionListenerList;
	private List<ServletRequestListener> servletRequestListenerList;
	private List<ServletRequestAttributeListener> servletRequestAttributeListenerList;

	public void addListener(Object listener) {
		if (listener instanceof ServletContextListener) {
			addServletContextListener((ServletContextListener) listener);
		}
		if (listener instanceof ServletContextAttributeListener) {
			addServletContextAttributeListener((ServletContextAttributeListener) listener);
		}
		if (listener instanceof HttpSessionActivationListener) {
			addHttpSessionActivationListener((HttpSessionActivationListener) listener);
		}
		if (listener instanceof HttpSessionAttributeListener) {
			addHttpSessionAttributeListener((HttpSessionAttributeListener) listener);
		}
		if (listener instanceof HttpSessionListener) {
			addHttpSessionListener((HttpSessionListener) listener);
		}
		if (listener instanceof ServletRequestListener) {
			addServletRequestListener((ServletRequestListener) listener);
		}
		if (listener instanceof ServletRequestAttributeListener) {
			addServletRequestAttributeListener((ServletRequestAttributeListener) listener);
		}
	}

	// --ServletRequestListener

	public List<ServletRequestListener> getServletRequestListener() {
		return servletRequestListenerList;
	}

	public void addServletRequestListener(ServletRequestListener listener) {
		if (servletRequestListenerList == null) {
			servletRequestListenerList = CollectionsUtil.newArrayList();
		}
		servletRequestListenerList.add(listener);
	}

	public void dispatchServletRequestListener_requestInitialized(
			ServletRequestEvent event) {
		if (servletRequestListenerList != null) {
			for (ServletRequestListener listener : servletRequestListenerList) {
				try {
					listener.requestInitialized(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchServletRequestListener_requestDestroyed(
			ServletRequestEvent event) {
		if (servletRequestListenerList != null) {
			for (ServletRequestListener listener : servletRequestListenerList) {
				try {
					listener.requestDestroyed(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	// --ServletRequestAttributeListener
	public List<ServletRequestAttributeListener> getServletRequestAttributeListenerList() {
		return servletRequestAttributeListenerList;
	}

	public void addServletRequestAttributeListener(
			ServletRequestAttributeListener listener) {
		if (servletRequestAttributeListenerList == null) {
			servletRequestAttributeListenerList = CollectionsUtil
					.newArrayList();
		}
		servletRequestAttributeListenerList.add(listener);
	}

	public void dispatchServletRequestAttributeListener_attributeAdded(
			ServletRequestAttributeEvent event) {
		if (servletRequestAttributeListenerList != null) {
			for (ServletRequestAttributeListener listener : servletRequestAttributeListenerList) {
				try {
					listener.attributeAdded(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchServletRequestAttributeListener_attributeRemoved(
			ServletRequestAttributeEvent event) {
		if (servletRequestAttributeListenerList != null) {
			for (ServletRequestAttributeListener listener : servletRequestAttributeListenerList) {
				try {
					listener.attributeRemoved(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchServletRequestAttributeListener_attributeReplaced(
			ServletRequestAttributeEvent event) {
		if (servletRequestAttributeListenerList != null) {
			for (ServletRequestAttributeListener listener : servletRequestAttributeListenerList) {
				try {
					listener.attributeReplaced(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	// --HttpSessionActivationListener

	public List<HttpSessionActivationListener> getHttpSessionActivationListenerList() {
		return httpSessionActivationListenerList;
	}

	public void addHttpSessionActivationListener(
			HttpSessionActivationListener listener) {
		if (httpSessionActivationListenerList == null) {
			httpSessionActivationListenerList = CollectionsUtil.newArrayList();
		}
		httpSessionActivationListenerList.add(listener);
	}

	public void dispatchHttpSessionActivationListener_sessionDidActivate(
			final HttpSessionEvent event) {
		if (httpSessionActivationListenerList != null) {
			for (HttpSessionActivationListener listener : httpSessionActivationListenerList) {
				listener.sessionDidActivate(event);
			}
		}
	}

	public void dispatchHttpSessionActivationListener_sessionWillPassivate(
			final HttpSessionEvent event) {
		if (httpSessionActivationListenerList != null) {
			for (HttpSessionActivationListener listener : httpSessionActivationListenerList) {
				listener.sessionWillPassivate(event);
			}
		}
	}

	// --HttpSessionAttributeListener

	public List<HttpSessionAttributeListener> getHttpSessionAttributeListenerList() {
		return httpSessionAttributeListenerList;
	}

	public void addHttpSessionAttributeListener(
			HttpSessionAttributeListener listener) {
		if (httpSessionAttributeListenerList == null) {
			httpSessionAttributeListenerList = CollectionsUtil.newArrayList();
		}
		httpSessionAttributeListenerList.add(listener);
	}

	public void dispatchHttpSessionAttributeListener_attributeAdded(
			final HttpSessionBindingEvent event) {
		if (httpSessionAttributeListenerList != null) {
			for (HttpSessionAttributeListener listener : httpSessionAttributeListenerList) {
				try {
					listener.attributeAdded(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchHttpSessionAttributeListener_attributeRemoved(
			final HttpSessionBindingEvent event) {
		if (httpSessionAttributeListenerList != null) {
			for (HttpSessionAttributeListener listener : httpSessionAttributeListenerList) {
				try {
					listener.attributeRemoved(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchHttpSessionAttributeListener_attributeReplaced(
			final HttpSessionBindingEvent event) {
		if (httpSessionAttributeListenerList != null) {
			for (HttpSessionAttributeListener listener : httpSessionAttributeListenerList) {
				try {
					listener.attributeReplaced(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	// --HttpSessionBindingListener

	public List<HttpSessionListener> getHttpSessionListenerList() {
		return httpSessionListenerList;
	}

	public void addHttpSessionListener(HttpSessionListener listener) {
		if (httpSessionListenerList == null) {
			httpSessionListenerList = CollectionsUtil.newArrayList();
		}
		httpSessionListenerList.add(listener);
	}

	public void dispatchHttpSessionListener_sessionCreated(
			HttpSessionEvent event) {
		if (httpSessionListenerList != null) {
			for (HttpSessionListener listener : httpSessionListenerList) {
				try {
					listener.sessionCreated(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}

			}
		}
	}

	public void dispatchHttpSessionListener_sessionDestroyed(
			HttpSessionEvent event) {
		if (httpSessionListenerList != null) {
			for (HttpSessionListener listener : httpSessionListenerList) {
				try {
					listener.sessionDestroyed(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	// --ServletContextAttributeListener

	public List<ServletContextAttributeListener> getServletContextAttributeListenerList() {
		return servletContextAttributeListenerList;
	}

	public void addServletContextAttributeListener(
			ServletContextAttributeListener listener) {
		if (servletContextAttributeListenerList == null) {
			servletContextAttributeListenerList = CollectionsUtil
					.newArrayList();
		}
		servletContextAttributeListenerList.add(listener);
	}

	public void dispatchServletContextAttributeListener_attributeAdded(
			ServletContextAttributeEvent event) {
		if (servletContextAttributeListenerList != null) {
			for (ServletContextAttributeListener listener : servletContextAttributeListenerList) {
				try {
					listener.attributeAdded(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchServletContextAttributeListener_attributeRemoved(
			ServletContextAttributeEvent event) {
		if (servletContextAttributeListenerList != null) {
			for (ServletContextAttributeListener listener : servletContextAttributeListenerList) {
				try {
					listener.attributeRemoved(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchServletContextAttributeListener_attributeReplaced(
			ServletContextAttributeEvent event) {
		if (servletContextAttributeListenerList != null) {
			for (ServletContextAttributeListener listener : servletContextAttributeListenerList) {
				try {
					listener.attributeReplaced(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	// --ServletContextListener

	public List<ServletContextListener> getServletContextListenerList() {
		return servletContextListenerList;
	}

	public void addServletContextListener(ServletContextListener listener) {
		if (servletContextListenerList == null) {
			servletContextListenerList = CollectionsUtil.newArrayList();
		}
		servletContextListenerList.add(listener);
	}

	public void dispatchServletContextListener_contextInitialized(
			ServletContextEvent event) {
		if (servletContextListenerList != null) {
			for (ServletContextListener listener : servletContextListenerList) {
				try {
					listener.contextInitialized(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}

	public void dispatchServletContextListener_contextDestroyed(
			ServletContextEvent event) {
		if (servletContextListenerList != null) {
			for (ServletContextListener listener : servletContextListenerList) {
				try {
					listener.contextDestroyed(event);
				} catch (Throwable t) {
					log.error(t.getMessage(), t);
				}
			}
		}
	}
}
