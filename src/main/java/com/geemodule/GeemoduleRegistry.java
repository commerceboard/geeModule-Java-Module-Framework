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
