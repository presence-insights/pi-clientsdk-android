/**
 * Copyright (c) 2015-2016 IBM Corporation. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.pi.geofence.demo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A generic in-memory caching utility.
 */
public class GenericCache {
    private final Map<String, Object> cache = new HashMap<>();
    private static final GenericCache instance = new GenericCache();

    private GenericCache() {
    }

    public static GenericCache getInstance() {
        return instance;
    }

    public <T> T get(String key, T def) {
        @SuppressWarnings("unchecked")
        T res = (T) cache.get(key);
        return (res == null) ? def : res;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T put(String key, T value) {
        return (T) cache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        return (T) cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public Set<String> keys() {
        return cache.keySet();
    }

    public Collection<?> values() {
        return cache.values();
    }

  /*
  public static void exampleUsage() {
    GenericCache cache = GenericCache.getInstance();
    int i = cache.get("toto");
    String s = cache.get("hello_string");
    cache.put("titi", 3);
    // one of the following 2 lines will fail at runtime with ClassCastException
    Date d = cache.get("field");
    String s2 = cache.get("field");
  }
  */
}
