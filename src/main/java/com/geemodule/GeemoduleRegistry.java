/*
 * Copyright 2016 the original author or authors.
 *
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

package com.geemodule;

import java.util.HashMap;
import java.util.Map;

public class GeemoduleRegistry {
    private static final ThreadLocal<Map<String, Object>> REGISTRY_THREAD_LOCAL = new ThreadLocal<Map<String, Object>>() {
        protected final Map<String, Object> initialValue() {
            return new HashMap<String, Object>();
        }
    };

    protected static final void put(final String key, final Object value) {
        registryMap().put(key, value);
    }

    @SuppressWarnings("unchecked")
    protected static final <T> T get(final String key) {
        return (T) registryMap().get(key);
    }

    protected static final void remove(final String key) {
        registryMap().remove(key);
    }

    protected static final void clear() {
        registryMap().clear();
    }

    public static final void cleanupThread() {
        clear();
        REGISTRY_THREAD_LOCAL.remove();
    }

    private static final Map<String, Object> registryMap() {
        return REGISTRY_THREAD_LOCAL.get();
    }
}
