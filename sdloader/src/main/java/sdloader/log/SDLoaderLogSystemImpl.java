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

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * System.outを行うログ
 * 
 * @author c9katayama
 * @author shot
 */
public class SDLoaderLogSystemImpl implements SDLoaderLog {

	private static final String DEBUG = "DEBUG";
	private static final String INFO = "INFO";
	private static final String WARN = "WARN";
	private static final String ERROR = "ERROR";
	private static final String FATAL = "FATAL";

	private String className;

	public SDLoaderLogSystemImpl(Class<?> c) {
		className = c.getName();
	}

	public void debug(Object log) {
		debug(log, null);
	}

	public void debug(Object log, Throwable t) {
		if (isDebugEnabled()) {
			log(DEBUG, log, t);
		}
	}

	public void error(Object log) {
		error(log, null);
	}

	public void error(Object log, Throwable t) {
		log(ERROR, className + " " + log, t);
	}

	public void info(Object log) {
		info(log, null);
	}

	public void info(Object log, Throwable t) {
		if (isInfoEnabled()) {
			log(INFO, log, null);
		}
	}

	public boolean isDebugEnabled() {
		// TODO DEBUGLEVEL
		return false;
	}

	public boolean isInfoEnabled() {
		return true;
	}

	public boolean isWarnEnabled() {
		return true;
	}

	public void warn(Object log) {
		warn(log, null);
	}

	public void warn(Object log, Throwable t) {
		if (isWarnEnabled()) {
			log(WARN, className + " " + log, t);
		}
	}

	public void fatal(Object log) {
		fatal(log, null);

	}

	public void fatal(Object log, Throwable t) {
		log(FATAL, className + " " + log, t);
	}

	private void log(String level, Object log, Throwable t) {
		String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
				.format(Calendar.getInstance().getTime());
		String logtext = "[" + level + "] " + time + " : ";
		if (log != null) {
			logtext += log.toString() + " ";
		}
		boolean systemOut = (level.equals(DEBUG) || level.equals(INFO));
		PrintStream w = systemOut ? System.out : System.err;
		if (t != null) {
			logtext += t.getMessage();
			w.println(logtext);
			t.printStackTrace();
		} else {
			w.println(logtext);
		}
	}

	public void release() {
		log(INFO, "logger released.", null);
	}

}
