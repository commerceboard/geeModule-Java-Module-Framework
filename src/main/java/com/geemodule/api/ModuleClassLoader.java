package com.geemodule.api;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

public interface ModuleClassLoader {
    public Module getModule();

    public URL findResource(String name);

    public Enumeration<URL> findResources(String name) throws IOException;

    public Class<?> loadClass(String name) throws ClassNotFoundException;
}