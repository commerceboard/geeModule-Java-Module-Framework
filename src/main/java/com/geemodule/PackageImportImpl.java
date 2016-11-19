package com.geemodule;

import org.osgi.framework.VersionRange;

import com.geemodule.api.PackageImport;

/**
 * Class containing prefix and version-range information of a package being imported into a module. Check out the OSGi documentation to find out how
 * the OSGi VersionRange class matches the OSGi Version class which is used in the export-package-class
 * {@link com.geemodule.PackageExportImpl}.
 * 
 * @author Michael Delamere
 */
public class PackageImportImpl implements PackageImport {
    private final String prefix;
    private final VersionRange versionRange;

    public PackageImportImpl(final String packageAndVersion) {
	if (packageAndVersion == null || "".equals(packageAndVersion.trim())) {
	    throw new ModuleException("Parameter packageAndVersion cannot be null in constructor");
	}

	if (packageAndVersion.contains("@version=")) {
	    String[] split = packageAndVersion.split("@version=");
	    this.prefix = split[0].trim();
	    this.versionRange = new VersionRange(split[1].trim());
	} else {
	    this.prefix = packageAndVersion.trim();
	    this.versionRange = new VersionRange("0.0.0");
	}
    }

    public final String getPrefix() {
	return prefix;
    }

    public final VersionRange getVersionRange() {
	return versionRange;
    }
}
