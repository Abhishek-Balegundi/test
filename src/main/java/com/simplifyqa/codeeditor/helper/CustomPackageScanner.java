package com.simplifyqa.codeeditor.helper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomPackageScanner {
    private static final Logger logger = Logger.getLogger(CustomPackageScanner.class.getName());
    public CustomPackageScanner() {}

    public static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException, IOException {
        logger.info("Retrieving classes from package: " + packageName);
        String path = packageName.replace('.', '/');

        ClassLoader classLoader = CustomPackageScanner.class.getClassLoader();

        Enumeration<URL> resources = classLoader.getResources(path);
        List<URL> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(resource);
        }

        List<Class<?>> classes = new ArrayList<>();
        for (URL directory : dirs) {
            logger.info("Directory: " + directory);
            String protocol = directory.getProtocol();
            if ("file".equals(protocol)) {
                File dir = new File(URLDecoder.decode(directory.getFile(), StandardCharsets.UTF_8));
                classes.addAll(findClasses(dir, packageName));
            } else if ("jar".equals(protocol)) {
                String filePath = directory.getFile();
                if (filePath.startsWith("file:")) {
                    filePath = filePath.substring(5, filePath.indexOf("!"));
                }
                try {
                    filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);
                    classes.addAll(findClassesInJar(new JarFile(filePath), path));
                } catch (IOException e) {
                    logger.log(Level.WARNING,String.format("Error decoding file path: %s, message: %s", filePath, e.getMessage()));
                }
            } else {
                logger.log(Level.WARNING,"Unhandled protocol: " + protocol);
            }
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            logger.log(Level.WARNING,"Directory does not exist: " + directory.getAbsolutePath());
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            logger.log(Level.WARNING,"No files in directory: " + directory.getAbsolutePath());
            return classes;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                try {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } catch (Exception e) {
                    logger.warning("Error occurred while adding the class from jar: "+e.getMessage());
                }
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (NoClassDefFoundError e) {
                    logger.warning("Error occurred while adding the class: "+e.getMessage());
                }
            }
        }
        return classes;
    }

    private static List<Class<?>> findClassesInJar(JarFile jarFile, String path) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entryName.startsWith(path) && entryName.endsWith(".class") && !entry.isDirectory()) {
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (NoClassDefFoundError e) {
                    logger.warning("Failed to load the class: "+className+" reason: "+e.getMessage());
                }
            }
        }
        return classes;
    }
}
