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

package com.geemodule.api;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

public interface ModuleLoader {
    public ModuleLoader bootstrap(String modulesRootDir);

    public ModuleLoader registerCache(ModuleCache cache);

    public Collection<Module> getLoadedModules();

    public Module getLoadedModule(String moduleName);

    public Module getLoadedModuleByCode(final String moduleCode);

    public Class<?> lookup(String className) throws ClassNotFoundException;

    public Class<?> lookup(String className, String moduleName) throws ClassNotFoundException;

    public Class<?> lookup(String className, String moduleName, String moduleVendor) throws ClassNotFoundException;

    public Class<?> lookup(String className, String moduleName, String moduleVendor, String versionRange) throws ClassNotFoundException;

    public Class<?> lookup(String className, String moduleName, String moduleVendor, String versionRange, Collection<Module> inModules) throws ClassNotFoundException;

    public Class<?> lookup(String className, String moduleName, String moduleVendor, String versionRange, Collection<Module> inModules, PackageImport forPackageImport, String ignoreModuleName)
        throws ClassNotFoundException;

    public Class<?> lookup(String className, Collection<Module> inModules) throws ClassNotFoundException;

    public Class<?> lookup(String className, Collection<Module> inModules, PackageImport forPackageImport) throws ClassNotFoundException;

    public Class<?> lookup(String className, Collection<Module> inModules, PackageImport forPackageImport, String ignoreModuleName) throws ClassNotFoundException;

    public Module locateModule(final String className, Collection<Module> inModules);

    public Module locateModule(final String className, PackageImport forPackageImport);

    public Module locateModule(final String className, Collection<Module> inModules, PackageImport forPackageImport);

    public Module locateModule(final String className, Collection<Module> inModules, PackageImport forPackageImport, String ignoreModuleName);

    public Class<?>[] findAllTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited);

    public ClassLoader[] getModuleClassLoaders();

    public String[] getPublicPackages();

    public boolean exportsPackage(Class<?> clazz);

    public boolean exportsPackage(String classOrPackageName);

    public boolean exportsPackage(String classOrPackageName, boolean exactMatch);

    public URL[] getPublicClasspath();

    public ClassLoader getPublicClassLoader();

    public ClassLoader getPublicClassLoader(ClassLoader fallbackClassLoader);

    public ClassLoader getPublicClassLoader(String packagePrefix, ClassLoader classLoader);

    public ClassLoader getPublicClassLoader(String packagePrefix, ClassLoader classLoader, ClassLoader fallbackClassLoader);

    public ClassLoader getPublicClassLoader(Map<String, ClassLoader> classLoaderMap);

    public ClassLoader getPublicClassLoader(Map<String, ClassLoader> classLoaderMap, ClassLoader fallbackClassLoader);
}