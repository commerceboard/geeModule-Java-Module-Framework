package com.geemodule;

import com.geemodule.api.*;
import org.osgi.framework.VersionRange;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleLoaderImpl implements ModuleLoader {
    private static final Logger LOG = Logger.getLogger(ModuleLoaderImpl.class.getName());

    private ModuleCache moduleCache = new ModuleCacheImpl();

    /**
     * Initializes all modules and resolves their dependencies.
     *
     * @param modulesRootDir Path to the root directory where all the modules reside.
     */
    @Override
    public final ModuleLoader bootstrap(final String modulesRootDir) {
        File rootDir = new File(modulesRootDir);

        // Check that root modules dir exists
        if (!rootDir.exists()) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Modules root dir '" + rootDir.getAbsolutePath() + "' found. Skipping.");
            }

            return this;
        }

        // Get all sub-module-directories
        File[] moduleDirs = rootDir.listFiles();

        // No sub directories (modules) exist
        if (moduleDirs.length == 0) {
            if (LOG.isLoggable(Level.INFO)) {
                LOG.info("No modules found in root dir '" + rootDir.getAbsolutePath() + "'.");
            }

            return this;
        } else {
            long startScanning = System.currentTimeMillis();

            // Go through all the directories and attempt to load the module.properties
            for (File moduleDir : moduleDirs) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Scanning directory '" + moduleDir + "'.");
                }

                // Attempt to load module configuration file (module.properties)
                Properties moduleConfig = getModuleConfig(moduleDir);

                if (moduleConfig != null) {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.finer("Attempting to load module '" + moduleDir + "'.");
                    }

                    // Initialize module with module path and configuration.
                    Module m;
                    try {
                        m = new ModuleImpl(moduleDir.getAbsolutePath(), moduleConfig, this);

                        // Only add to modules list if it is active.
                        if (m.isActive()) {
                            Module cachedModule = cache().putIfAbsent(m.toUniqueId(), m);

                            if (cachedModule != null)
                                m = cachedModule;
                        }
                    } catch (Throwable t) {
                        LOG.throwing(ModuleImpl.class.getName(), "constructor", t);
                    }
                }
            }

            for (Module module : cache().getAll()) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Initialization of " + module.toUniqueId() + " complete.");
                }
            }

            long endScanning = System.currentTimeMillis();

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Completed module scanning in " + (endScanning - startScanning) + "ms. " + cache().size() + " modules found.");
            }

            // Now we attempt to resolve all module dependencies

            long startResolving = System.currentTimeMillis();

            resolveDependencies();

            long endResolving = System.currentTimeMillis();

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Completed module dependency resolving in " + (endResolving - startResolving) + "ms.");
            }
        }

        return this;
    }

    @Override
    public final ModuleLoader registerCache(final ModuleCache cache) {
        if (moduleCache == null) {
            moduleCache = cache;
        }

        return this;
    }

    protected final ModuleCache cache() {
        return moduleCache;
    }

    @Override
    public final Collection<Module> getLoadedModules() {
        return cache().getAll();
    }

    @Override
    public final Module getLoadedModule(final String moduleName) {
        if (moduleName == null)
            return null;

        Collection<Module> modules = getLoadedModules();

        Module foundModule = null;

        for (Module module : modules) {
            if (moduleName.equals(module.getName())) {
                foundModule = module;
                break;
            }
        }

        return foundModule;
    }

    @Override
    public final Module getLoadedModuleByCode(final String moduleCode) {
        if (moduleCode == null)
            return null;

        Collection<Module> modules = getLoadedModules();

        Module foundModule = null;

        for (Module module : modules) {
            if (moduleCode.equals(module.getCode())) {
                foundModule = module;
                break;
            }
        }

        return foundModule;
    }

    public final Class<?> lookupResource(final String className) {

        return null;
    }

    @Override
    public final ClassLoader[] getModuleClassLoaders() {
        List<ClassLoader> moduleClassLoaders = new ArrayList<ClassLoader>();

        for (Module module : cache().getAll()) {
            moduleClassLoaders.addAll(Arrays.asList(module.getModuleClassLoader()));
        }

        return moduleClassLoaders.toArray(new ClassLoader[moduleClassLoaders.size()]);
    }

    @Override
    public final String[] getPublicPackages() {
        List<String> publicPackages = new ArrayList<String>();

        for (Module module : cache().getAll()) {
            publicPackages.addAll(Arrays.asList(module.getPublicPackages()));
        }

        return publicPackages.toArray(new String[publicPackages.size()]);
    }

    @Override
    public final boolean exportsPackage(Class<?> clazz) {
        if (clazz == null)
            return false;

        return exportsPackage(clazz.getName(), false);
    }

    @Override
    public final boolean exportsPackage(String classOrPackageName) {
        return exportsPackage(classOrPackageName, false);
    }

    @Override
    public final boolean exportsPackage(String classOrPackageName, boolean exactMatch) {
        if(classOrPackageName == null)
            return false;

        String[] packages = getPublicPackages();

        for (String aPackage : packages) {
            if (exactMatch && aPackage.equals(classOrPackageName)) {
                return true;
            } else if (!exactMatch && classOrPackageName.startsWith(aPackage)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public final URL[] getPublicClasspath() {
        List<URL> publicClasspaths = new ArrayList<URL>();

        for (Module module : cache().getAll()) {
            publicClasspaths.addAll(Arrays.asList(module.getPublicClasspath()));
        }

        return publicClasspaths.toArray(new URL[publicClasspaths.size()]);
    }

    @Override
    public final ClassLoader getPublicClassLoader() {
        return new PublicClassLoader(this);
    }

    @Override
    public final ClassLoader getPublicClassLoader(ClassLoader fallbackClassLoader) {
        return new PublicClassLoader(this, fallbackClassLoader);
    }

    @Override
    public final ClassLoader getPublicClassLoader(String packagePrefix, ClassLoader classLoader) {
        return new PublicClassLoader(this, packagePrefix, classLoader);
    }

    @Override
    public final ClassLoader getPublicClassLoader(String packagePrefix, ClassLoader classLoader, ClassLoader fallbackClassLoader) {
        return new PublicClassLoader(this, packagePrefix, classLoader, fallbackClassLoader);
    }

    @Override
    public final ClassLoader getPublicClassLoader(Map<String, ClassLoader> classLoaderMap) {
        return new PublicClassLoader(this, classLoaderMap);
    }

    @Override
    public final ClassLoader getPublicClassLoader(Map<String, ClassLoader> classLoaderMap, ClassLoader fallbackClassLoader) {
        return new PublicClassLoader(this, classLoaderMap, fallbackClassLoader);
    }

    @Override
    public final Class<?>[] findAllTypesAnnotatedWith(final Class<? extends Annotation> annotation, final boolean honorInherited) {
        List<Class<?>> types = new ArrayList<Class<?>>();

        for (Module module : cache().getAll()) {
            if (module.isActive()) {
                Class<?>[] annotatedTypes = module.findTypesAnnotatedWith(annotation, honorInherited);

                if (annotatedTypes != null) {
                    types.addAll(Arrays.asList(annotatedTypes));
                }
            }
        }

        return types.toArray(new Class[types.size()]);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className) throws ClassNotFoundException {
        return lookup(className, (String) null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final String moduleName) throws ClassNotFoundException {
        return lookup(className, moduleName, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final String moduleName, final String moduleVendor) throws ClassNotFoundException {
        return lookup(className, moduleName, moduleVendor, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final String moduleName, final String moduleVendor, final String versionRange) throws ClassNotFoundException {
        return lookup(className, moduleName, moduleVendor, versionRange, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final String moduleName, final String moduleVendor, final String versionRange, final Collection<Module> inModules) throws ClassNotFoundException {
        return lookup(className, moduleName, moduleVendor, versionRange, inModules, null, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final Collection<Module> inModules) throws ClassNotFoundException {
        return lookup(className, inModules, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final Collection<Module> inModules, final PackageImport forPackageImport) throws ClassNotFoundException {
        return lookup(className, null, null, null, inModules, forPackageImport, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#lookup(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Class<?> lookup(final String className, final Collection<Module> inModules, final PackageImport forPackageImport, final String ignoreModuleName) throws ClassNotFoundException {
        return lookup(className, null, null, null, inModules, forPackageImport, ignoreModuleName);
    }

    /**
     * Attempts to load a class from the module found in
     * {@link com.geemodule.ModuleLoaderImpl#locateModule(String, String, String, String, Module[], PackageImport, String)}
     *
     * @see com.geemodule.ModuleLoaderImpl#locateModule(String, String, String, String, Module[], PackageImport, String)
     */
    @Override
    public final Class<?> lookup(final String className, final String moduleName, final String moduleVendor, final String versionRange, Collection<Module> inModules, final PackageImport forPackageImport, final String ignoreModuleName) throws ClassNotFoundException {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Looking up '" + className + "' with lookup parameters [moduleName=" + moduleName + ", moduleVendor=" + moduleVendor + ", versionRange=" + versionRange + "].");
        }

        Module m = locateModule(className, moduleName, moduleVendor, versionRange, inModules, forPackageImport, ignoreModuleName);

        Class<?> c = null;

        if (m != null) {
            c = m.loadClass(className);
        }

        if (c == null) {
            throw new ClassNotFoundException();
        }

        return c;
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#locateModule(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Module locateModule(final String className, final Collection<Module> inModules) {
        return locateModule(className, inModules, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#locateModule(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Module locateModule(final String className, final PackageImport forPackageImport) {
        return locateModule(className, null, forPackageImport);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#locateModule(String, String, String, String, Module[], PackageImport)
     */
    @Override
    public final Module locateModule(final String className, final Collection<Module> inModules, final PackageImport forPackageImport) {
        return locateModule(className, null, null, null, inModules, forPackageImport, null);
    }

    /**
     * @see com.geemodule.ModuleLoaderImpl#locateModule(String, String, String, String, Module[], PackageImport, String
     * ignoreModuleName)
     */
    @Override
    public final Module locateModule(final String className, final Collection<Module> inModules, final PackageImport forPackageImport, final String ignoreModuleName) {
        return locateModule(className, null, null, null, inModules, forPackageImport, ignoreModuleName);
    }

    /**
     * Attempts to locate a module according to the parameters passed to this method. The more specific the request is, the more likely it is to get
     * exactly the module one wants. The minimum requirement it to provide the class-name.
     *
     * @param className        Locates module that contains this class-name. This is the only required parameter. The rest are optional.
     * @param moduleName       The module must have this exact name.
     * @param moduleVendor     The module's vendor.
     * @param versionRange     The module's version must match the specified version-range. Checkout out the OSGi-VersionRange documentation for more information.
     * @param inModules        Only attempt to locate module using the passed in module array instead of searching in all modules.
     * @param forPackageImport Only attempt to locate module in modules that have a matching ModulePackageExport.
     * @return Module
     */
    protected final Module locateModule(final String className, final String moduleName, final String moduleVendor, final String versionRange, final Collection<Module> inModules, final PackageImport forPackageImport, final String ignoreModuleName) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Locating module for parameters [className=" + className + ", moduleName=" + moduleName + ", moduleVendor=" + moduleVendor + ", versionRange=" + versionRange + "].");
        }

        if (className == null)
            throw new NullPointerException("The parameter className cannot be null");

        List<Module> foundMatchingModules = new ArrayList<Module>();

        Module locatedModule = null;

        Collection<Module> searchInModules = inModules == null ? cache().getAll() : inModules;

        for (Module module : searchInModules) {
            // No need to process further if module is inactive
            if (!module.isActive())
                continue;

            // If we do not want a particular module, ignore it. This is necessary when locating dependencies in order
            // to avoid endless loops where the module keeps on calling itself again and again.
            if (ignoreModuleName != null && ignoreModuleName.equals(module.getName()))
                continue;

            // If package-import was specified and the current module does not have a matching export, skip this module
            if (forPackageImport != null && !module.hasMatchingPackageExport(forPackageImport.getPrefix(), forPackageImport.getVersionRange()))
                continue;

            // Find out if module is exposing a package that matches the class name to lookup
            PackageExport[] packageExports = module.getExportPackages();

            boolean moduleMatches = false;

            for (PackageExport packageExport : packageExports) {
                if (className.startsWith(packageExport.getPrefix())) {
                    // check module name, if it was provided
                    if (moduleName != null && !moduleName.equals(module.getName()))
                        continue;

                    // check module vendor, if it was provided
                    if (moduleVendor != null && !moduleVendor.equals(module.getVendor()))
                        continue;

                    // check module version, if it was provided
                    if (versionRange != null && !new VersionRange(versionRange).includes(packageExport.getVersion()))
                        continue;

                    moduleMatches = true;
                }
            }

            if (moduleMatches) {
                foundMatchingModules.add(module);

                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Looking up '" + className + "' with lookup parameters [moduleName=" + moduleName + ", moduleVendor=" + moduleVendor + ", versionRange=" + versionRange + "].");
                }
            }
        }

        // No matching module found
        if (foundMatchingModules.size() == 0) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("No matching module found for class: " + className);
            }

            return null;
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("(1) Initial order of modules:");
            for (Module module : foundMatchingModules) {
                LOG.finest("(1) " + module.toUniqueId());
            }
        }

        // Most of the time, only 1 module will match. This is the easiest case, no further searching required.
        // Obviously, the more parameters that are passed to this method, the more likely it is that we only end up with
        // 1 result. Passing only the className increases the chance of finding more than one module containing an
        // appropriate export-package-definition considerably.
        if (foundMatchingModules.size() == 1) {
            locatedModule = foundMatchingModules.get(0);
        }
        // Should there be more than 1 module, we have to decide on one:
        // 1. Find the most specific package
        // 2. If more than one of the most specific package exists, find the highest version with the help of the
        // OSGi-Version class.
        else if (foundMatchingModules.size() > 1) {
            // Sort the found modules so that the most specific package is at the top (which may be more than one)
            // In this case we simply make use of the String.compareTo() method.
            Collections.sort(foundMatchingModules, new Comparator<Module>() {
                public int compare(Module m1, Module m2) {
                    String p1 = m1.findClosestMatchingPackageExport(className);
                    String p2 = m2.findClosestMatchingPackageExport(className);

                    return p1 == null && p2 == null ? 0 : p1 == null ? -1 : p1.compareTo(p2);
                }
            });

            Collections.reverse(foundMatchingModules);

            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("(2) Order of modules after package names have been sorted (most specific packages at the top):");
                for (Module module : foundMatchingModules) {
                    LOG.finest("(2) " + module.toUniqueId());
                }
            }

            // Pop out the first module with the most specific package name for comparison
            Module firstModule = foundMatchingModules.get(0);

            List<Module> modulesWithIdenticalMostSpecificPackage = new ArrayList<Module>();
            modulesWithIdenticalMostSpecificPackage.add(firstModule);

            // Find other modules with the same most specific package name
            for (Module module : foundMatchingModules) {
                // First one is already in the list
                if (module.equals(firstModule))
                    continue;

                String firstPackageName = firstModule.findClosestMatchingPackageExport(className);
                String packageName = module.findClosestMatchingPackageExport(className);

                if (packageName != null && packageName.equals(firstPackageName)) {
                    modulesWithIdenticalMostSpecificPackage.add(module);
                }
            }

            // If there are more than one, then sort by version using the OSGi Version.compareTo() method
            if (modulesWithIdenticalMostSpecificPackage.size() > 1) {
                Collections.sort(modulesWithIdenticalMostSpecificPackage);
                Collections.reverse(modulesWithIdenticalMostSpecificPackage);
            }

            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("(3) Order of modules after packages have been sorted by version-number (highest version-number at the top):");
                for (Module module : modulesWithIdenticalMostSpecificPackage) {
                    LOG.finest("(3) " + module.toUniqueId());
                }
            }

            // The newest most specific package should now be at the top
            locatedModule = modulesWithIdenticalMostSpecificPackage.get(0);
        }

        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Returning located module " + locatedModule);
        }

        return locatedModule;
    }

    /**
     * Resolves dependencies for all modules
     */
    private final void resolveDependencies() {
        // Iterate through all the modules and find out what dependency they want to import
        for (Module importingModule : cache().getAll()) {
            List<Module> moduleDependencies = new ArrayList<Module>();

            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Resolving dependencies for module: " + importingModule.toUniqueId());
            }

            PackageImport[] moduleImports = importingModule.getImportPackages();

            // No dependencies, so just continue
            if (moduleImports == null)
                continue;

            // Now iterate through all the imports and find matching exports in other modules
            for (PackageImport moduleImport : moduleImports) {
                // Now iterate through *other* modules and check their exports
                for (Module exportingModule : cache().getAll()) {
                    // Make sure that we are looking in *other* modules
                    if (!exportingModule.equals(importingModule)) {
                        if (exportingModule.hasMatchingPackageExport(moduleImport.getPrefix(), moduleImport.getVersionRange())) {
                            if (LOG.isLoggable(Level.FINEST)) {
                                LOG.finest("Adding dependency '" + exportingModule.toUniqueId() + "' to '" + importingModule.toUniqueId() + "'");
                            }

                            moduleDependencies.add(exportingModule);
                        }
                    }
                }
            }

            // Add dependencies to module
            setDependencies(importingModule, moduleDependencies);
        }
    }

    private final void setDependencies(final Module module, final Collection<Module> dependencies) {
        // Add dependencies to module via reflection, so that we do not need to add a public setter method to the
        // interface.
        try {
            Field field = module.getClass().getDeclaredField("dependencies");
            field.setAccessible(true);
            field.set(module, dependencies);
        } catch (Throwable t) {
            throw new RuntimeException(t.getMessage(), t);
        }
    }

    /**
     * Loads the module configuration from the module.properties found in the module's directory.
     *
     * @param moduleDir File module directory
     * @return Properties module configuration
     */
    private final Properties getModuleConfig(final File moduleDir) {
        Properties moduleConfig = new Properties();

        File[] foundModuleProps = moduleDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.equals("module.properties");
            }
        });

        if (foundModuleProps == null || foundModuleProps.length == 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("No module.properties configuration file found in module directory '" + moduleDir.getAbsolutePath() + "'.");
            }

            return null;
        } else if (foundModuleProps.length > 1) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning("More than 1 module.properties configuration file found in module directory '" + moduleDir.getAbsolutePath() + "'. Skipping module.");
            }

            return null;
        } else {
            try {
                moduleConfig.load(new FileInputStream(foundModuleProps[0]));
            } catch (FileNotFoundException e) {
                throw new ModuleException(e);
            } catch (IOException e) {
                throw new ModuleException(e);
            }
        }

        return moduleConfig;
    }
}
