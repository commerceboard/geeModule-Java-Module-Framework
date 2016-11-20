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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.stream.Collectors;

import com.geemodule.ModuleClassLoaderImpl.HotSwappableClass;
import com.geemodule.api.Module;
import com.geemodule.api.ModuleClassLoader;

public class HotSwapClassWatcher extends TimerTask {
    private final Module module;
    private final ModuleClassLoader moduleClassLoader;
    private final Map<String, HotSwappableClass> hotSwappableClasses;

    public HotSwapClassWatcher(Module module, ModuleClassLoader moduleClassLoader, Map<String, HotSwappableClass> hotSwappableClasses) {
        this.module = module;
        this.moduleClassLoader = moduleClassLoader;
        this.hotSwappableClasses = hotSwappableClasses;

        System.out.println("Starting new HotSwapClassWatcher for: " + module.getName());
    }

    @Override
    public void run() {
        Path classesPath = module.getClassesDir();
        String basePath = classesPath.toString().replace('\\', '/');

        try {
            List<Path> classPaths = Files.walk(classesPath).filter(p -> p.getFileName().toString().endsWith(".class")).collect(Collectors.toList());

            for (Path path : classPaths) {
                String className = path.toString().replace('\\', '/');
                className = className.replace(basePath.toString(), "").replace('/', '.');
                className = className.replace(".class", "");

                if (className.startsWith("."))
                    className = className.substring(1);

                Long currentTime = path.toFile().lastModified();
                HotSwappableClass hsw = hotSwappableClasses.get(className);

                if (className.contains("cart"))
                    System.out.println("className " + className + " - " + currentTime + " - " + (hsw == null ? null : hsw.lastModifiedTime));

                if (hsw != null && currentTime != null && currentTime > hsw.lastModifiedTime) {
                    System.out.println("!!!!! FOUND MODIFIED CLASS FILE !!! " + className);

                    try {
                        ((ModuleClassLoaderImpl) moduleClassLoader).reloadClass(className);
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        } catch (IOException e) {
        }
    }
}
