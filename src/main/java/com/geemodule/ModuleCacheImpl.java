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
