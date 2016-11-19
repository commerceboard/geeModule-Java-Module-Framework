package com.geemodule.api;

import java.util.Collection;
import java.util.Set;

public interface ModuleCache {
    public Module putIfAbsent(String key, Module module);

    public Module get(String key);

    public boolean containsKey(String key);

    public void remove(String key);

    public Set<String> keySet();

    public Collection<Module> getAll();

    public int size();
}
