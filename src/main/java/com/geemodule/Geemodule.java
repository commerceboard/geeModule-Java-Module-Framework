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

import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.geemodule.api.ModuleCache;
import com.geemodule.api.ModuleLoader;

/**
 * Central class for creating the ModuleLoader.
 * 
 * @author Michael Delamere
 * 
 */
public final class Geemodule {
    private static final Logger LOG = Logger.getLogger(Geemodule.class.getName());

    private static volatile Map<String, ModuleLoader> moduleLoaderCache = new ConcurrentHashMap<>();

    private Geemodule() {
    }

    /**
     * Created the modules-loader without specifying the root-dmodules-path as a
     * parameter. See {@link com.geemodule.Geemodule} to find
     * out how the root-modules-path is determined.
     */
    public static final ModuleLoader createModuleLoader() {
        return createModuleLoader(null, null);
    }

    public static final ModuleLoader createModuleLoader(final String rootModulesPath) {
        return createModuleLoader(rootModulesPath, null);
    }

    /**
     * Creates the module-loader and calls its bootstrap method which
     * initializes all modules and their dependencies. The modules-root-dir is
     * determined in the following order:
     * <ol>
     * <li>The modules-root-dir parameter passed into this method.</li>
     * <li>If parameter does not exist, try to retrieve it from the
     * geemodule.properties. The geemodule.properties must either reside in the
     * root of
     * the class-path or its location must be specified as a system property
     * with the key 'geemodule.properties.path'. The latter has precedence.</li>
     * <li>If the properties file cannot be found or the entry
     * 'root.module.path' is missing, attempt to load the value with
     * System.getProperty("root.module.path").</li>
     * <li>Should none of the above work an IllegalStateException is thrown and
     * the modules cannot be initialized.</li>
     * </ol>
     */
    public static final ModuleLoader createModuleLoader(final String rootModulesPath, final ModuleCache moduleCache) {
        String _rootModulesPath = rootModulesPath;

        // Root modules path not passed as parameter. Lets check the
        // geemodule.properties file.
        if (Str.isEmpty(_rootModulesPath)) {
            // Has as specific location for the properties file been specified?
            String geemodulePropertiesPath = System.getProperty("geemodule.properties.path");

            Properties props = new Properties();

            // Use specific location if it exists.
            if (!Str.isEmpty(geemodulePropertiesPath)) {
                try {
                    props.load(new FileInputStream(geemodulePropertiesPath));
                    _rootModulesPath = props.getProperty("root.module.path");
                } catch (Exception e) {
                    throw new IllegalStateException("Geemodule config could not be found at the specified path: " + geemodulePropertiesPath, e);
                }
            }
            // Otherwise try the standard location which is the classpath root.
            else {
                try {
                    props.load(Geemodule.class.getClassLoader().getResourceAsStream("geemodule.properties"));
                    _rootModulesPath = props.getProperty("root.module.path");
                } catch (Exception e) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("No properties file found in default classpath location.");
                    }
                }
            }
        }

        // If it is still empty, try finding the root modules path in the
        // system.
        if (Str.isEmpty(_rootModulesPath)) {
            _rootModulesPath = System.getProperty("root.module.path");
        }

        if (Str.isEmpty(_rootModulesPath)) {
            throw new IllegalStateException("Unable to create ModuleLoader because no root modules path could be determined");
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Creating ModuleLoader with root modules location '" + _rootModulesPath + "'.");
            }
        }

        ModuleLoader moduleLoader = moduleLoaderCache.get(_rootModulesPath);

        if (moduleLoader == null) {
            synchronized (_rootModulesPath) {
                moduleLoader = moduleLoaderCache.get(_rootModulesPath);

                if (moduleLoader == null) {
                    moduleLoader = new ModuleLoaderImpl();
                    moduleLoader = moduleLoader.bootstrap(_rootModulesPath);
                    ModuleLoader cachedModuleLoader = moduleLoaderCache.putIfAbsent(_rootModulesPath, moduleLoader);

                    if (cachedModuleLoader != null)
                        moduleLoader = cachedModuleLoader;
                }
            }
        }

        return moduleLoader;
    }
}
