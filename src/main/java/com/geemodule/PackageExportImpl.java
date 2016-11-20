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

import org.osgi.framework.Version;

import com.geemodule.api.PackageExport;

/**
 * Class containing prefix and version information of a package exported by a
 * module. Only exported packages can be consumed by other modules when
 * they define a matching package-import
 * {@link com.geemodule.PackageImportImpl}.
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
