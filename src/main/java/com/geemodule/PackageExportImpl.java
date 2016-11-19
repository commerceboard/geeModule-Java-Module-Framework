package com.geemodule;

import org.osgi.framework.Version;

import com.geemodule.api.PackageExport;

/**
 * Class containing prefix and version information of a package exported by a module. Only exported packages can be consumed by other modules when
 * they define a matching package-import {@link com.geemodule.PackageImportImpl}.
 * 
 * @author Michael Delamere
 */
public class PackageExportImpl implements PackageExport {
    private final String prefix;
    private final Version version;

    public PackageExportImpl(final String packagePrefix, final Version version) {
	this.prefix = packagePrefix.trim();
	this.version = version;
    }

    public final String getPrefix() {
	return prefix;
    }

    public final Version getVersion() {
	return version;
    }
}
