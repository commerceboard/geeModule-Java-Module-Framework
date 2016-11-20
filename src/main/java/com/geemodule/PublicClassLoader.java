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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.geemodule.api.ModuleLoader;

public class PublicClassLoader extends ClassLoader {
    static {
        try {
            registerAsParallelCapable();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final String[] STANDARD_PACKAGES = new String[] { "java.", "javax.", "sun.", "com.sun.", "oracle." };
    private final ModuleLoader moduleLoader;
    private final Map<String, ClassLoader> classLoaderMap;
    private final ClassLoader fallbackClassLoader;

    public PublicClassLoader(ModuleLoader moduleLoader) {
        this.moduleLoader = moduleLoader;
        this.classLoaderMap = new LinkedHashMap<>();
        this.fallbackClassLoader = null;
    }

    public PublicClassLoader(ModuleLoader moduleLoader, ClassLoader fallbackClassLoader) {
        this.moduleLoader = moduleLoader;
        this.classLoaderMap = new LinkedHashMap<>();
        this.fallbackClassLoader = fallbackClassLoader;
    }

    public PublicClassLoader(ModuleLoader moduleLoader, String packagePrefix, ClassLoader classLoader) {
        this.moduleLoader = moduleLoader;
        this.classLoaderMap = new LinkedHashMap<>();
        this.classLoaderMap.put(packagePrefix, classLoader);
        this.fallbackClassLoader = null;
    }

    public PublicClassLoader(ModuleLoader moduleLoader, String packagePrefix, ClassLoader classLoader, ClassLoader fallbackClassLoader) {
        this.moduleLoader = moduleLoader;
        this.classLoaderMap = new LinkedHashMap<>();
        this.classLoaderMap.put(packagePrefix, classLoader);
        this.fallbackClassLoader = fallbackClassLoader;
    }

    public PublicClassLoader(ModuleLoader moduleLoader, Map<String, ClassLoader> classLoaderMap) {
        this.moduleLoader = moduleLoader;
        this.classLoaderMap = classLoaderMap;
        this.fallbackClassLoader = null;
    }

    public PublicClassLoader(ModuleLoader moduleLoader, Map<String, ClassLoader> classLoaderMap, ClassLoader fallbackClassLoader) {
        this.moduleLoader = moduleLoader;
        this.classLoaderMap = classLoaderMap;
        this.fallbackClassLoader = fallbackClassLoader;
    }

    @Override
    public final URL findResource(final String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Enumeration<URL> findResources(final String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public final synchronized Class<?> loadClass(final String name) throws ClassNotFoundException {
        // If the class has already been loaded, just return that
        Class<?> c = findLoadedClass(name);

        if (c != null) {
            return c;
        }

        boolean isLocalMode = Boolean.getBoolean("cp.localmode");

        if (!isStandardClass(name) && !isLocalMode && classLoaderMap != null && !classLoaderMap.isEmpty()) {
            Set<String> keys = classLoaderMap.keySet();

            boolean foundMatch = false;

            for (String packagePrefix : keys) {
                if (name.startsWith(packagePrefix)) {
                    foundMatch = true;

                    try {
                        ClassLoader cl = classLoaderMap.get(packagePrefix);
                        c = cl.loadClass(name);

                        return c;
                    } catch (Throwable t) {
                    }
                }
            }

            if (foundMatch && c == null)
                throw new ClassNotFoundException(name);

            if (foundMatch)
                return c;
        }

        // Don't bother dealing with module specific stuff if we are looking for
        // a standard class
        if (isStandardClass(name) || isLocalMode) {
            try {
                c = super.loadClass(name);
            } catch (Throwable t) {
            }

            if (c != null)
                return c;
        }

        // Now see if we can find the class from the modules
        try {
            c = moduleLoader.lookup(name);
        } catch (Throwable t) {
            if (fallbackClassLoader == null)
                throw new ClassNotFoundException(name, t);
        }

        if (c == null && fallbackClassLoader != null) {
            c = fallbackClassLoader.loadClass(name);
        }

        return c;
    }

    private final boolean isStandardClass(final String name) {
        boolean isStandardClass = false;

        for (String standardPackagePrefix : STANDARD_PACKAGES) {
            if (name.startsWith(standardPackagePrefix)) {
                isStandardClass = true;
                break;
            }
        }

        return isStandardClass;
    }
}
