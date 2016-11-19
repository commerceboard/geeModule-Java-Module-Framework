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

    public Class<?> lookup(String className, String moduleName, String moduleVendor, String versionRange, Collection<Module> inModules, PackageImport forPackageImport, String ignoreModuleName) throws ClassNotFoundException;

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