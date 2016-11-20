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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.geemodule.api.Module;
import com.geemodule.api.ModuleCache;

public class ModuleCacheImpl implements ModuleCache {
    private final Map<String, Module> moduleCache = new ConcurrentHashMap<>();

    @Override
    public Module putIfAbsent(String key, Module module) {
        if (module == null)
            return null;

        return moduleCache.putIfAbsent(key, module);
    }

    @Override
    public Module get(String key) {
        return moduleCache.get(key);
    }

    @Override
    public boolean containsKey(String key) {
        return moduleCache.containsKey(key);
    }

    @Override
    public void remove(String key) {
        moduleCache.remove(key);
    }

    @Override
    public Set<String> keySet() {
        return moduleCache.keySet();
    }

    @Override
    public Collection<Module> getAll() {
        return moduleCache.values();
    }

    @Override
    public int size() {
        return moduleCache.size();
    }
}
