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

import com.geemodule.api.Module;
import com.geemodule.api.ModuleClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * ClassLoader responsible for loading classes and resources from modules.
 */
public class ModuleClassLoaderImpl extends URLClassLoader implements ModuleClassLoader {
    static {
        try {
            registerAsParallelCapable();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static final class HotSwappableClass {
        protected Class<?> clazz;
        protected long lastModifiedTime;

        protected HotSwappableClass(Class<?> clazz, long lastModifiedTime) {
            this.clazz = clazz;
            this.lastModifiedTime = lastModifiedTime;
        }
    }

    private static final String[] STANDARD_PACKAGES = new String[] { "java.", "javax.", "sun.", "com.sun.", "oracle.", "com.yourkit." };

    private final Module module;

    private final Map<String, HotSwappableClass> hotSwappableClasses = new HashMap<>();
    private final List<String> nonSwappableClasses = new ArrayList<>();

    public ModuleClassLoaderImpl(final Module module) throws MalformedURLException {
        super(module.getClasspath(), Geemodule.class.getClassLoader());

        this.module = module;

        // TODO: Hot swapping.
        // Timer timer = new Timer();
        // timer.schedule(new HotSwapClassWatcher(module, this,
        // hotSwappableClasses), 30000, 10000);
    }

    public ModuleClassLoaderImpl(final URL[] urls) {
        super(urls, Geemodule.class.getClassLoader());

        this.module = null;
    }

    @Override
    public final Module getModule() {
        return module;
    }

    @Override
    public final URL findResource(final String name) {
        Path resourcePath = module.locateResource(name);

        if (resourcePath != null && Files.exists(resourcePath)) {
            try {
                return resourcePath.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return super.findResource(name);
    }

    @Override
    public final Enumeration<URL> findResources(final String name) throws IOException {
        return super.findResources(name);
    }

    /**
     * Attempts to load a class in the following order:
     * <ol>
     * <li>If class has already been loaded, then just return that.</li>
     * <li>If not and the class is a standard-class (java.*, javax.*), just use
     * the parent ClassLoader.</li>
     * <li>If it is not a standard java class, attempt to it from the
     * module.</li>
     * <li>If the class was not found in this module, attempt to find it in one
     * of its dependency modules.</li>
     * <li>If all the above fail, we fallback to the parent ClassLoader.</li>
     * <li>If the class cannot be found with any ClassLoader, a
     * ClassNotFoundException is thrown.</li>
     * </ol>
     */
    @Override
    public final Class<?> loadClass(final String name) throws ClassNotFoundException {
        Class<?> c = null;
        boolean isStandardClass;

        synchronized (getClassLoadingLock(name)) {
            c = findLoadedClass(name);

            if (c != null) {
                return c;
            }

            isStandardClass = isStandardClass(name);

            // TODO: Hot swapping.
            // If the class has already been loaded, just return that
            // if (isStandardClass || nonSwappableClasses.contains(name))
            // {
            // c = findLoadedClass(name);
            //
            // if (c != null)
            // {
            // return c;
            // }
            // }

            // TODO: Hot swapping.
            // If hot-swapping is enabled, see if we already have a loaded class
            // in our map.
            // HotSwappableClass hsw = hotSwappableClasses.get(name);
            //
            // if (hsw != null)
            // {
            // return hsw.clazz;
            // }

            boolean isLocalMode = Boolean.getBoolean("cp.localmode");

            // Don't bother dealing with module specific stuff if we are looking
            // for a standard class
            if (isStandardClass || isLocalMode) {
                try {
                    c = super.loadClass(name);
                } catch (Throwable t) {
                }

                return c;
            }

            // Now see if we can find the class from the current module
            try {
                // TODO: Hot swapping.
                // PackageExport[] packageExports = module.getExportPackages();
                //
                // for (PackageExport pe : packageExports)
                // {
                // if (name.startsWith(pe.getPrefix()))
                // {
                // c = loadSwappableClass(name);
                // break;
                // }
                // }

                if (c == null)
                    c = findClass(name);
            } catch (Throwable t) {
                // Class not found locally. Try other modules next.
            }

            // If the class is not in the current module, try loading it from a
            // dependency module
            if (c == null && !isStandardClass && module.hasDependencies()) {
                try {
                    // String loadClassKey = loadClassKey(name);
                    // Integer loadCount = GeemoduleRegistry.get(loadClassKey);
                    //
                    // if (loadCount == null)
                    // {
                    // GeemoduleRegistry.put(loadClassKey, Integer.valueOf(1));
                    // }
                    // else if (loadCount > 100)
                    // {
                    // System.out.println("WARN: The class '" + name +
                    // "' is being loaded more than a 100 times by the module '"
                    // + module.getName() + "'.");
                    // }
                    // else
                    // {
                    // GeemoduleRegistry.put(loadClassKey, loadCount++);
                    // }

                    c = module.loadClassFromDependency(name);
                } catch (Throwable t) {
                    // Class not found in other modules. Try parent ClassLoader
                    // next.
                }
            }

            if (c == null && module.isImportPackagesFromContainer()) {
                try {
                    c = module.loadClassFromContainer(name);
                } catch (Throwable t) {
                    // Class not found in container ClassLoader. No other
                    // ClassLoader to try. Giving up.
                }
            }
        }

        if (c == null) {
            throw new ClassNotFoundException("[" + module.toUniqueId() + "] Class '" + name + "' could not be found.");
        }

        // if (!isStandardClass && !hotSwappableClasses.containsKey(name) &&
        // nonSwappableClasses.contains(name))
        // nonSwappableClasses.add(name);

        return c;
    }

    private Class<?> loadSwappableClass(String name) throws ClassNotFoundException {
        // it is assumed to be that the class is already available in the
        // available classes map
        try {
            File classFile = new File(module.getClassesDir().toString(), convertToDefinitionName(name));

            if (classFile.exists()) {
                byte[] bytes = Files.readAllBytes(classFile.toPath());

                StringBuilder hswName = new StringBuilder(name).append(classFile.lastModified());

                System.out.println(hswName.toString());

                Class<?> clazz = defineClass(hswName.toString(), bytes, 0, bytes.length);

                hotSwappableClasses.put(name, new HotSwappableClass(clazz, classFile.lastModified()));

                return clazz;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }

        return null;
    }

    protected Class<?> reloadClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            return loadSwappableClass(name);
        }
    }

    private String convertToDefinitionName(String className) {
        return className.replaceAll("\\.", "/") + ".class";
    }

    /**
     * Checks to see if the class that we are trying to load is a standard
     * java-class (java.*, javax.*). If that is the case, then there is no need
     * to
     * go through all the module logic, just use the parent-class-loader.
     *
     * @param name
     *            Class that we are trying to load
     * @return boolean isStandardClass
     */
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

    private final String loadClassKey(final String name) {
        return new StringBuilder(name).append('@').append(module.getName()).toString();
    }
}
