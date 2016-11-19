package com.geemodule.api;

import org.osgi.framework.Version;

public interface PackageExport {
    public String getPrefix();

    public Version getVersion();
}