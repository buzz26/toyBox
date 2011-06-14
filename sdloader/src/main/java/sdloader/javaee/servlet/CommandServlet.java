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
package sdloader.javaee.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sdloader.SDLoader;
import sdloader.http.ProcessScopeContext;
import sdloader.util.PathUtil;

@SuppressWarnings("serial")
public class CommandServlet extends HttpServlet {

	public static final String COMMAND_STOP = "stop";

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {

		String command = PathUtil.removeStartSlashIfNeed(req.getPathInfo());
		if (command != null) {
			if (command.equals(COMMAND_STOP)) {
				SDLoader loader = ProcessScopeContext.getContext()
						.getSDLoader();
				loader.stop();
				return;
			}
		}
		res.getWriter().print("Invalide command.");
	}
}
