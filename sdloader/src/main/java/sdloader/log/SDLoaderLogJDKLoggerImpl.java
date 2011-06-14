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
package sdloader.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDKのLoggerを使ったLog実装
 * 
 * @author c9katayama
 */
public class SDLoaderLogJDKLoggerImpl implements SDLoaderLog {

	private Logger logger;

	public SDLoaderLogJDKLoggerImpl(Class<?> c) {
		logger = Logger.getLogger(c.getName());
	}

	protected String format(Object log, Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter pw = new PrintWriter(stringWriter);
		if (log != null) {
			pw.print(log.toString());
		}
		if (t != null) {
			pw.println();
			t.printStackTrace(pw);
		}
		pw.flush();
		pw.close();
		return stringWriter.toString();
	}

	public void debug(Object log) {
		logger.config(format(log, null));
	}

	public void debug(Object log, Throwable t) {
		logger.config(format(log, t));
	}

	public void info(Object log) {
		logger.info(format(log, null));
	}

	public void info(Object log, Throwable t) {
		logger.info(format(log, t));
	}

	public void warn(Object log) {
		logger.warning(format(log, null));
	}

	public void warn(Object log, Throwable t) {
		logger.warning(format(log, t));
	}

	public void error(Object log) {
		logger.severe(format(log, null));
	}

	public void error(Object log, Throwable t) {
		logger.severe(format(log, t));
	}

	public void fatal(Object log) {
		logger.severe(format(log, null));

	}

	public void fatal(Object log, Throwable t) {
		logger.severe(format(log, t));

	}

	public boolean isDebugEnabled() {
		return logger.isLoggable(Level.CONFIG);
	}

	public boolean isInfoEnabled() {
		return logger.isLoggable(Level.INFO);
	}

	public boolean isWarnEnabled() {
		return logger.isLoggable(Level.SEVERE);
	}

	public void release() {
		logger = null;
	}
}
