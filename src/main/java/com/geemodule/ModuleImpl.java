package com.geemodule;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import com.geemodule.annotation.Bootstrapable;
import com.geemodule.api.Module;
import com.geemodule.api.ModuleBootstrap;
import com.geemodule.api.ModuleLoader;
import com.geemodule.api.PackageExport;
import com.geemodule.api.PackageImport;
import com.geemodule.util.Strings;

/**
 * Domain object for holding module data and finding its classes and resources.
 *
 * @author Michael Delamere
 * @see com.geemodule.Geemodule
 * @see com.geemodule.ModuleLoader
 * @see com.geemodule.ModuleClassLoader
 */
public class ModuleImpl implements Module {
    private final String vendor;

    private final String name;

    private final String description;

    private final Version version;

    private final PackageImport[] importPackages;

    private final PackageExport[] exportPackages;

    private final boolean isActive;

    private final boolean importPackagesFromContainer;

    private final PackageImport[] importContainerPackages;

    private final String basePath;

    private final ClassLoader moduleClassLoader;

    private final String classesDir = "classes";

    private final String libDir = "lib";

    private final String resourcesDir = "resources";

    private final String webDir = "web";

    private Collection<Module> dependencies = null;

    private final ModuleLoader moduleLoader;

    private Reflections reflections = null;

    private final Map<String, Class<?>[]> annotatedTypesCache = new HashMap<>();

    private static final String CSV_DELIMITER = ";";

    private static final Logger LOG = Logger.getLogger(ModuleImpl.class.getName());

    @SuppressWarnings("unchecked")
    public ModuleImpl(final String modulePath, final Properties moduleConfig, final ModuleLoader moduleLoader) throws MalformedURLException {
        if (moduleConfig == null || moduleConfig.isEmpty()) {
            throw new ModuleException("Unable to initialize module because module configuration is null or empty.");
        }

        this.moduleLoader = moduleLoader;

        // Remember path to module
        this.basePath = modulePath;

        // Set properties from module.properties
        this.vendor = moduleConfig.getProperty("module.vendor");

        if (this.vendor == null || "".equals(this.vendor.trim()))
            throw new ModuleException("Module '" + basePath + "' could not be loaded because property 'module.vendor' is missing in the module definition file module.properties");

        this.name = moduleConfig.getProperty("module.name");

        if (this.name == null || "".equals(this.name.trim()))
            throw new ModuleException("Module '" + basePath + "' could not be loaded because property 'module.name' is missing in the module definition file module.properties");

        if (moduleConfig.getProperty("module.version") != null && !"".equals(moduleConfig.getProperty("module.version"))) {
            this.version = new Version(moduleConfig.getProperty("module.version").trim());
        } else {
            throw new ModuleException("Module '" + basePath + "' could not be loaded because property 'module.version' is missing in the module definition file module.properties");
        }

        this.isActive = Boolean.valueOf(moduleConfig.getProperty("module.active", "false"));

        this.description = moduleConfig.getProperty("module.description");

        // Iterate through imports that may have been defined
        String importPackageStr = moduleConfig.getProperty("module.import.package");
        if (importPackageStr != null && !"".equals(importPackageStr.trim())) {
            List<PackageImport> mpList = new ArrayList<PackageImport>();

            String[] importPackageArr = importPackageStr.split(CSV_DELIMITER);
            for (String importPackage : importPackageArr) {
                mpList.add(new PackageImportImpl(importPackage));
            }

            this.importPackages = mpList.toArray(new PackageImport[mpList.size()]);
        } else {
            this.importPackages = null;
        }

        // Does the module want to use classes from the container using Geemodule?
        this.importPackagesFromContainer = Boolean.valueOf(moduleConfig.getProperty("module.container.import.active", "false"));

        if (this.importPackagesFromContainer) {
            // Iterate through container imports that may have been defined
            String importContainerPackageStr = moduleConfig.getProperty("module.container.import.package");
            if (importContainerPackageStr != null && !"".equals(importContainerPackageStr.trim())) {
                List<PackageImport> cpList = new ArrayList<PackageImport>();

                String[] importContainerPackageArr = importContainerPackageStr.split(CSV_DELIMITER);
                for (String importContainerPackage : importContainerPackageArr) {
                    cpList.add(new PackageImportImpl(importContainerPackage));
                }

                this.importContainerPackages = cpList.toArray(new PackageImport[cpList.size()]);
            } else {
                this.importContainerPackages = null;
            }
        } else {
            this.importContainerPackages = null;
        }

        // Iterate through exports that may have been defined
        String exportPackageStr = moduleConfig.getProperty("module.export.package");
        if (exportPackageStr != null && !"".equals(exportPackageStr.trim())) {
            List<PackageExport> mpList = new ArrayList<PackageExport>();

            String[] exportPackageArr = exportPackageStr.split(CSV_DELIMITER);
            for (String exportPackage : exportPackageArr) {
                mpList.add(new PackageExportImpl(exportPackage, this.version));
            }

            this.exportPackages = mpList.toArray(new PackageExport[mpList.size()]);
        } else {
            this.exportPackages = null;
        }

        // try
        // {
        // If we are in local-mode we need to use the local ClassLoader.
        boolean isLocalMode = Boolean.getBoolean("cp.localmode");

        if (isLocalMode) {
            this.moduleClassLoader = this.getClass().getClassLoader();
        } else {
            this.moduleClassLoader = new ModuleClassLoaderImpl(this);
        }
        // }
        // catch (MalformedURLException e)
        // {
        // LOG.throwing("ModuleImpl", "ModuleImpl", e);
        // }

        if (!this.isActive && LOG.isLoggable(Level.FINER)) {
            LOG.finer("Module '" + toUniqueId() + "' was found and appears to be conform, but it will not be loaded as it has not been activated (module.active=true).");
        }

        if (this.isActive) {
            Class<ModuleBootstrap>[] bootstrapClasses = (Class<ModuleBootstrap>[]) this.findTypesAnnotatedWith(Bootstrapable.class, false);

            // System.out.println("Module '" + toUniqueId() + ":: " + Arrays.asList(bootstrapClasses) + " -> " +
            // this.getClass().getClassLoader());

            if (bootstrapClasses != null && bootstrapClasses.length > 0) {
                for (Class<ModuleBootstrap> bootstrap : bootstrapClasses) {
                    try {
                        if (LOG.isLoggable(Level.FINER)) {
                            LOG.finer("Starting module bootstrap class: " + bootstrap.getName());
                        }
                        // System.out.println("Starting module bootstrap class: " + bootstrap);

                        bootstrap.newInstance().startup();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Loads class using the ModuleClassLoader.
     *
     * @param name Fully qualified name of class that we are trying to load.
     * @return Class<?>
     */
    @Override
    public final Class<?> loadClass(final String name) throws ClassNotFoundException {
        return this.moduleClassLoader.loadClass(name);
    }

    /**
     * Attempts to load a class from one of its dependencies.
     *
     * @param name Fully qualified name of class that we are trying to load.
     * @return Class<?>
     */
    @Override
    public final Class<?> loadClassFromDependency(final String name) throws ClassNotFoundException {
        Class<?> c = null;

        if (hasDependencies()) {
            PackageImport packageImport = findPackageImport(name);

            if (packageImport != null) {
                c = moduleLoader.lookup(name, dependencies, packageImport, getName());

                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("[" + toUniqueId() + "] Class '" + name + "' found in module dependency class-loader " + c.getClassLoader());
                }
            }
        }

        return c;
    }

    /**
     * Attempts to load a class from the container class-loader.
     *
     * @param name Fully qualified name of class that we are trying to load.
     * @return Class<?>
     */
    @Override
    public final Class<?> loadClassFromContainer(final String name) throws ClassNotFoundException {
        Class<?> c = null;

        if (importPackagesFromContainer) {
            PackageImport packageContainerImport = findContainerPackageImport(name);

            if (packageContainerImport != null) {
                // c = Class.forName(name, false, moduleLoader.getClass().getClassLoader());
                c = Geemodule.class.getClassLoader().loadClass(name);

                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("[" + toUniqueId() + "] Class '" + name + "' found in container class-loader " + c.getClassLoader());
                }
            }
        }

        return c;
    }

    /**
     * Finds the best matching package-import definition which would have been specified under this modules' module.properties.
     *
     * @param className Fully qualified class name
     * @return ModulePackageImport
     */
    @Override
    public final PackageImport findPackageImport(final String className) {
        PackageImport foundModulePackage = null;

        if (importPackages != null && importPackages.length > 0) {
            for (PackageImport packageImport : importPackages) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("[" + toUniqueId() + "] Attempting to match package-import '" + packageImport.getPrefix() + "' with class name '" + className + "'");
                }

                if (className.startsWith(packageImport.getPrefix())) {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer("[" + toUniqueId() + "] Found matching package-import '" + packageImport.getPrefix() + "' for class '" + className + "'");
                    }

                    foundModulePackage = packageImport;
                    break;
                }
            }
        }

        return foundModulePackage;
    }

    private final PackageImport findContainerPackageImport(final String className) {
        PackageImport foundContainerPackage = null;

        if (importContainerPackages != null && importContainerPackages.length > 0) {
            for (PackageImport packageImport : importContainerPackages) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("[" + toUniqueId() + "] Attempting to match container-package-import '" + packageImport.getPrefix() + "' with class name '" + className + "'");
                }

                if (className.startsWith(packageImport.getPrefix())) {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer("[" + toUniqueId() + "] Found matching container-package-import '" + packageImport.getPrefix() + "' for class '" + className + "'");
                    }

                    foundContainerPackage = packageImport;
                    break;
                }
            }
        }

        return foundContainerPackage;
    }

    /**
     * Looks for a matching package-export definition for the specified package-prefix and version range. Typically this method is used to find an
     * export matching a package-import from another module.
     *
     * @param packagePrefix The exported package to look for.
     * @param versionRange  The version-range to look for according to the OSGi-VersionRange rules.
     */
    @Override
    public final boolean hasMatchingPackageExport(final String packagePrefix, final VersionRange versionRange) {
        if (exportPackages == null || exportPackages.length == 0)
            return false;

        boolean hasMatchingExportPackage = false;

        for (PackageExport packageExport : exportPackages) {
            if (packageExport.getPrefix().startsWith(packagePrefix)) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("[" + toUniqueId() + "] Package match found. Now checking import-version '" + versionRange + "' against exported version '" + packageExport.getVersion() + "'");
                }

                if (versionRange.includes(packageExport.getVersion())) {
                    hasMatchingExportPackage = true;

                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("[" + toUniqueId() + "] Import-version '" + versionRange + "' matches exported version '" + packageExport.getVersion() + "' for package-prefix '" + packagePrefix + "'");
                    }
                }
            }
        }

        return hasMatchingExportPackage;
    }

    /**
     * Finds a package-export where the package-prefix fits best to the class-name passed to this method. For example: if we were looking for the
     * class com.a.b.c.Example then a ModulePackageExport with a package-prefix of com.a.b.c would be a closer match than one that only has com.a.b.
     *
     * @param className Class-name to find closest package-prefix for.
     * @return closest matching package
     */
    @Override
    public final String findClosestMatchingPackageExport(final String className) {
        if (className == null)
            throw new NullPointerException();

        List<String> matchingPackages = new ArrayList<String>();

        String closestMatchingPackage = null;

        if (exportPackages != null && exportPackages.length > 0) {
            for (PackageExport packageExport : exportPackages) {
                if (className.startsWith(packageExport.getPrefix())) {
                    matchingPackages.add(packageExport.getPrefix());
                }
            }

            if (matchingPackages.size() > 1) {
                Collections.sort(matchingPackages);
                Collections.reverse(matchingPackages);
                closestMatchingPackage = matchingPackages.get(0);
            } else if (matchingPackages.size() > 0) {
                closestMatchingPackage = matchingPackages.get(0);
            }
        }

        return closestMatchingPackage;
    }

    /**
     * A unique identification for this module. It must only exist once in the modules-root-directory.
     */
    @Override
    public final String toUniqueId() {
        StringBuffer id = new StringBuffer();
        id.append(vendor);
        id.append(".").append(name);
        id.append("@version=").append(version);

        return id.toString().replace(' ', '-');
    }

    @Override
    public Collection<Module> getDependencies() {
        return this.dependencies;
    }

    @Override
    public boolean hasDependencies() {
        return this.dependencies != null && this.dependencies.size() > 0;
    }

    /**
     * Classpath array containing the modules' classes folder and all the jars in the module/lib folder.
     *
     * @return URL[]
     */
    @Override
    public final URL[] getClasspath() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(new File(basePath, classesDir).toURI().toURL());
        urls.addAll(findJars());

        return urls.toArray(new URL[urls.size()]);
    }

    @Override
    public final URL[] getPublicClasspath() {
        List<URL> urls = new ArrayList<URL>();

        if (exportPackages != null && exportPackages.length > 0) {
            for (PackageExport packageExport : exportPackages) {
                try {
                    String packageExportResource = toResource(packageExport.getPrefix());

                    Enumeration<URL> resourceUrls = moduleClassLoader.getResources(packageExportResource);

                    File classesPath = new File(basePath, classesDir);

                    while (resourceUrls.hasMoreElements()) {
                        URL url = resourceUrls.nextElement();

                        int idx = url.toExternalForm().lastIndexOf(packageExportResource);

                        if (idx != -1) {
                            File f = new File(classesPath.getAbsolutePath(), url.toExternalForm().substring(idx));
                            urls.add(f.toURI().toURL());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return urls.toArray(new URL[urls.size()]);
    }

    @Override
    public final String[] getPublicPackages() {
        List<String> publicPackages = new ArrayList<String>();

        if (exportPackages != null && exportPackages.length > 0) {
            for (PackageExport packageExport : exportPackages) {
                publicPackages.add(packageExport.getPrefix());
            }
        }

        return publicPackages.toArray(new String[publicPackages.size()]);
    }

    private final String toResource(final String packagePrefix) {
        if (packagePrefix == null)
            return null;

        String resource = packagePrefix.replace('.', '/');
        resource.replace('\\', '/');

        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        }

        return resource;
    }

    @Override
    public final Class<?>[] findTypesAnnotatedWith(final Class<? extends Annotation> annotation, final boolean honorInherited) {
        String cacheKey = new StringBuilder(annotation.getName()).append("_").append(String.valueOf(honorInherited)).toString();

        Class<?>[] annotatedTypes = null;

        // read.lock();

        try {
            annotatedTypes = annotatedTypesCache.get(cacheKey);
        } finally {
            // read.unlock();
        }

        if (annotatedTypes != null) {
            return annotatedTypes;
        } else {
            Set<Class<?>> types = getReflections().getTypesAnnotatedWith(annotation, honorInherited);

            if (types != null) {
                annotatedTypes = types.toArray(new Class[types.size()]);

                // write.lock();

                try {
                    annotatedTypesCache.put(cacheKey, annotatedTypes);
                } finally {
                    // write.unlock();
                }
            }

            return annotatedTypes;
        }
    }

    private final Reflections getReflections() {
        if (reflections == null) {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.addUrls(getPublicClasspath()).addClassLoader(getModuleClassLoader()).setScanners(new TypeAnnotationsScanner(), new SubTypesScanner());

            this.reflections = new Reflections(cb);
        }

        return this.reflections;
    }

    /**
     * Find all the jars in the modules' lib folder.
     *
     * @returnList<URL>
     */
    private final List<URL> findJars() {
        List<URL> libUrls = new ArrayList<URL>();

        File f = new File(basePath, libDir);

        if (f.exists()) {
            File[] libs = f.listFiles();

            if (libs != null && libs.length > 0) {
                for (File lib : libs) {
                    try {
                        libUrls.add(lib.toURI().toURL());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return libUrls;
    }

    @Override
    public final boolean isImportPackagesFromContainer() {
        return importPackagesFromContainer;
    }

    @Override
    public final String getVendor() {
        return vendor;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getCode() {
        return Strings.slugify(name).toLowerCase();
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final Version getVersion() {
        return version;
    }

    @Override
    public final PackageImport[] getImportPackages() {
        return importPackages;
    }

    @Override
    public final PackageExport[] getExportPackages() {
        return exportPackages;
    }

    @Override
    public final boolean isActive() {
        return isActive;
    }

    @Override
    public final String getBasePath() {
        return basePath;
    }

    @Override
    public final ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    @Override
    public final Path getClassesDir() {
        return new File(basePath, classesDir).toPath();
    }

    @Override
    public Path getLibDir() {
        return new File(basePath, libDir).toPath();
    }

    @Override
    public Path getResourcesDir() {
        return new File(basePath, resourcesDir).toPath();
    }

    @Override
    public Path getWebDir() {
        return new File(basePath, webDir).toPath();
    }

    @Override
    public Path locateResource(String relativeResourcePath) {
        if (relativeResourcePath == null || relativeResourcePath.isEmpty())
            return null;

        if (relativeResourcePath.startsWith(webDir)) {
            return getWebDir().resolve(relativeResourcePath.substring(4));
        } else {
            return getResourcesDir().resolve(relativeResourcePath);
        }
    }

    /**
     * This compareTo() method only compares the OSGi-Version class. See the OSGi documentation for ordering rule.
     */
    @Override
    public final int compareTo(Module otherModule) {
        return version.compareTo(otherModule.getVersion());
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj != null && obj instanceof Module) {
            return this.toUniqueId().equals(((Module) obj).toUniqueId());
        } else {
            return false;
        }
    }
}
