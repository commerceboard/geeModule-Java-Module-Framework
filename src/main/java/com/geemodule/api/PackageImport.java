package com.geemodule.api;

import org.osgi.framework.VersionRange;

public interface PackageImport {
    public String getPrefix();

    public VersionRange getVersionRange();
}