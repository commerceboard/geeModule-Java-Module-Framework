# geeModule - Java Module Framework

**geeModule is a Java framework for managing modules.**
It requires no special building or packaging and instead loads classes and resources directly from their directories.
geeModule uses classloader isolation (like Tomcat or OSGi) to control what modules see or expose to other modules.
Each module can also have their own set of libraries to avoid classpath clashes.
