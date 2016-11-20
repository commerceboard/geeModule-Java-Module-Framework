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

import org.osgi.framework.VersionRange;

import com.geemodule.api.PackageImport;

/**
 * Class containing prefix and version-range information of a package being
 * imported into a module. Check out the OSGi documentation to find out how
 * the OSGi VersionRange class matches the OSGi Version class which is used in
 * the export-package-class
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
