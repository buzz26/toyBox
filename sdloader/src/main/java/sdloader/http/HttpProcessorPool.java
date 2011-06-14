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

import java.util.List;
import java.util.Set;

import sdloader.SDLoader;
import sdloader.log.SDLoaderLog;
import sdloader.log.SDLoaderLogFactory;
import sdloader.util.CollectionsUtil;

/**
 * HttpProcessorPool
 * 
 * @author c9katayama
 * @author shot
 */
public class HttpProcessorPool {

	private static final SDLoaderLog log = SDLoaderLogFactory
			.getLog(HttpProcessor.class);

	private int nextProcessorNo;

	private int maxThreadPoolNum;

	private List<HttpProcessor> processorPool = CollectionsUtil.newLinkedList();

	private Set<HttpProcessor> borrowProcessorSet = CollectionsUtil
			.newHashSet();

	private SDLoader sdLoader;

	public HttpProcessorPool(SDLoader loader, int maxThreadPoolNum) {
		this.sdLoader = loader;
		this.maxThreadPoolNum = maxThreadPoolNum;
		for (int i = 0; i < maxThreadPoolNum; ++i) {
			String processorName = "HttpProcessor:init" + i;
			HttpProcessor processor = createProcessor(processorName);
			processorPool.add(processor);
		}
		nextProcessorNo = maxThreadPoolNum + 1;
	}

	public synchronized HttpProcessor borrowProcessor() {
		synchronized (this) {
			HttpProcessor processor = null;
			if (processorPool.isEmpty()) {
				String processorName = "HttpProcessor:" + (nextProcessorNo++);
				processor = createProcessor(processorName);
			} else {
				processor = processorPool.remove(0);
				log.debug("reuse " + processor.getName());
			}
			borrowProcessorSet.add(processor);
			return processor;
		}
	}

	public void returnProcessor(HttpProcessor processor) {
		synchronized (this) {
			borrowProcessorSet.remove(processor);
			if (processorPool.size() < maxThreadPoolNum) {
				log.debug("return " + processor.getName());
				processorPool.add(processor);
			} else {
				log.debug("stop " + processor.getName());
				processor.stopProcessor();
			}
		}
	}

	public void close() {
		synchronized (this) {
			for (HttpProcessor processor : processorPool) {
				processor.stopProcessor();
			}
			processorPool.clear();
			for (HttpProcessor borrowProcessor : borrowProcessorSet) {
				borrowProcessor.stopProcessor();
			}
			borrowProcessorSet.clear();
		}
	}

	private HttpProcessor createProcessor(String processorName) {
		HttpProcessor processor = new HttpProcessor(processorName, sdLoader,
				this);
		processor.start();
		log.debug("create " + processor.getName());
		return processor;
	}
}
