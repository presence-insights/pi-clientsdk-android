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

package com.ibm.pisdk.geofencing.rest;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A generic map with string keys and values of any type.
 */
public class GenericMap implements Serializable {
    /**
     * A {@link java.util.Map} backing this data object to store the attributes and their values.
     */
    private final Map<String, Object> map = new TreeMap<String, Object>();

    /**
     * Get the value of the attribute witht he specified name.
     * @param key the name of the attribute.
     * @param <T> the type of the attribute.
     * @return the attribute's value, or <code>null</code> if no attribute with this name exists.
     */
    @SuppressWarnings("unchecked")
    protected <T> T get(String key) {
        return (T) map.get(key);
    }

    /**
     * Put the specified attribute value in this data object.
     * <p>This mehotd is package-protected so as not to be exposed to clients.
     * To expose it publicly, it must be be overriden with a 'public' qualifier in subclasses.
     * @param key the name of the attribute.
     * @param value the value of the attribute
     * @param <T> the type of the attribute.
     * @return the previous value, or <code>null</code> if no value existed previously.
     */
    @SuppressWarnings("unchecked")
    protected <T> T set(String key, T value) {
        return (T) map.put(key, value);
    }

    /**
     * Remove the specified attribute.
     * <p>This mehotd is package-protected so as not to be exposed to clients.
     * To expose it publicly, it must be be overriden with a 'public' qualifier in subclasses.
     * @param key the name of the attribute.
     * @param <T> the type of the attribute.
     * @return the attribute's value, or <code>null</code> if no attribute with this name exists.
     */
    @SuppressWarnings("unchecked")
    protected <T> T remove(String key) {
        return (T) map.remove(key);
    }

    /**
     * Determine whether this map contains the specified key.
     * @param key the key to check.
     * @return <code>true</code> if this map ocntains the key, <code>false</code> otherwise.
     */
    protected boolean has(String key) {
        return map.containsKey(key);
    }

    /**
     * Get a set of all the attribute names in this content item.
     * @return the attribute names as a <code>Set</code> of strings.
     */
    protected Set<String> keys() {
        return map.keySet();
    }

    /**
     * Get a set of all the attribute values in this content item.
     * @return the values as a collection of objects.
     */
    protected Collection<Object> values() {
        return map.values();
    }

    /**
     * Remove all entries in this content item,
     * making it effectively blank and ready to be reused.
     */
    protected void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append(map).toString();
    }
}
