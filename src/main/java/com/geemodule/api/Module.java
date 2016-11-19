package com.geemodule.api;

import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

public interface Module extends Comparable<Module> {
    public Class<?> loadClass(String name) throws ClassNotFoundException;

    public String toUniqueId();

    public String getVendor();

    public String getName();

    public String getCode();

    public String getDescription();

    public Version getVersion();

    public PackageImport[] getImportPackages();

    public PackageExport[] getExportPackages();

    public boolean isActive();

    public String getBasePath();

    public ClassLoader getModuleClassLoader();

    public Path getClassesDir();

    public Path getLibDir();

    public Path getWebDir();

    public Path getResourcesDir();

    public Path locateResource(String relativeResourcePath);

    public PackageImport findPackageImport(String className);

    public boolean hasMatchingPackageExport(String packagePrefix, VersionRange versionRange);

    public boolean isImportPackagesFromContainer();

    public Collection<Module> getDependencies();

    public boolean hasDependencies();

    public URL[] getClasspath() throws MalformedURLException;

    public URL[] getPublicClasspath();

    public String[] getPublicPackages();

    public String findClosestMatchingPackageExport(String className);

    public Class<?> loadClassFromDependency(String name) throws ClassNotFoundException;

    public Class<?> loadClassFromContainer(String name) throws ClassNotFoundException;

    public Class<?>[] findTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited);
}