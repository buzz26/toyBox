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
package sdloader.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 
 * @author AKatayama
 * 
 */
public class CollectionsUtil {

	public static <T> ArrayList<T> newArrayList() {
		return new ArrayList<T>();
	}

	public static <T> ArrayList<T> newArrayList(Collection<T> collection) {
		return new ArrayList<T>(Assertion.notNull(collection));
	}

	public static <T> LinkedList<T> newLinkedList() {
		return new LinkedList<T>();
	}

	public static <K, V> HashMap<K, V> newHashMap() {
		return new HashMap<K, V>();
	}

	public static <K, V> HashMap<K, V> newHashMap(final int capacity) {
		return new HashMap<K, V>(capacity);
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
		return new ConcurrentHashMap<K, V>();
	}

	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(
			final int capacity) {
		return new ConcurrentHashMap<K, V>(capacity);
	}

	public static <T> ConcurrentLinkedQueue<T> newConcurrentLinkedQueue() {
		return new ConcurrentLinkedQueue<T>();
	}

	public static <T> Vector<T> newVector() {
		return new Vector<T>();
	}

	public static <T> HashSet<T> newHashSet() {
		return new HashSet<T>();
	}

	public static <T> Stack<T> newStack() {
		return new Stack<T>();
	}

}
