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

/**
 * ログインターフェース
 * 
 * @author c9katayama
 */
public interface SDLoaderLog {

	public boolean isDebugEnabled();

	public boolean isInfoEnabled();

	public boolean isWarnEnabled();

	public void debug(Object log);

	public void debug(Object log, Throwable t);

	public void info(Object log);

	public void info(Object log, Throwable t);

	public void warn(Object log);

	public void warn(Object log, Throwable t);

	public void error(Object log);

	public void error(Object log, Throwable t);

	public void fatal(Object log);

	public void fatal(Object log, Throwable t);

	void release();
}
