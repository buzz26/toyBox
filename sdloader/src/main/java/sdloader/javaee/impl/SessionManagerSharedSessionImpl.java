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

import javax.servlet.http.HttpSession;

import sdloader.javaee.InternalWebApplication;
import sdloader.javaee.SessionManager;

/**
 * セッションIDに係わらず、すべてのリクエストに対して 同一のセッションを返します.
 * 
 * <pre>
 * プロセスの異なるIEなどでセッション共有できます。
 * </pre>
 * 
 * @author c9katayama
 * @author shot
 */
public class SessionManagerSharedSessionImpl extends SessionManager {

	private static HttpSessionImpl session;

	public synchronized HttpSession getSession(String sessionId,
			boolean createNew, InternalWebApplication webApplication) {
		if (session != null) {
			if (!session.isInvalidate()) {
				return session;
			} else {
				session = null;
				if (createNew) {
					return createNewSession(webApplication);
				} else {
					return null;
				}
			}
		} else {
			if (createNew) {
				return createNewSession(webApplication);
			} else {
				return null;
			}
		}
	}

	protected HttpSession createNewSession(InternalWebApplication webApplication) {
		String sessionId = createNewSessionId();
		HttpSessionImpl ses = new HttpSessionImpl(webApplication, sessionId);
		session = ses;
		return ses;
	}

	@Override
	public void close() {
		session.invalidate();
		session = null;
	}
}
