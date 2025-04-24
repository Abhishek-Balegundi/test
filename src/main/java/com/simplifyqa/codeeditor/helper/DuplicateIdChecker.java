package com.simplifyqa.codeeditor.helper;

import com.simplifyqa.pluginbase.codeeditor.annotations.SyncAction;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DuplicateIdChecker {
    private static final Logger logger = Logger.getLogger(DuplicateIdChecker.class.getName());
    private static final Map<String, Method> methodsList = new HashMap<>();
    private static boolean buildStatus = true;

    public static void main(String[] args) throws Exception {
        String packageName = "com.simplifyqa.codeeditor";
        List<Class<?>> classes = CustomPackageScanner.getClasses(packageName);
        logger.info("\u001B[1m\u001B[32m" + "[INFO]" + "\u001B[0m"+"Fetching Unique Ids from classes: " + classes);
        registerMethodsFromClass(classes);
        if (!buildStatus) {
            throw new RuntimeException("Build failed since encountered above defined error while fetching for duplicated unique ids");
        }
        logger.info("\u001B[1m\u001B[32m" + "[INFO]" + "\u001B[0m"+"\u001B[1m\u001B[32m" + "---------------BUILD SUCCESS---------------" + "\u001B[0m");
        logger.info("\u001B[1m\u001B[32m" + "[INFO]" + "\u001B[0m"+"\u001B[1m\u001B[32m" + "---------No duplicate Unique Id is found---------" + "\u001B[0m");
    }

    private static void registerMethodsFromClass(List<Class<?>> classes) {
        for (Class<?> eachClass : classes) {
            for (Method method : eachClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SyncAction.class)) {
                    if (!Modifier.isPublic(method.getModifiers())){
                        logger.log(Level.SEVERE, "\u001B[31m" + "METHOD: "+method.getName()+" is not public, but is annotated with @SyncAction, Please make it public." + "\u001B[0m");
                        buildStatus=false;
                    }
                    SyncAction annotation = method.getAnnotation((SyncAction.class));
                    String uniqueId = annotation.uniqueId();
                    if (methodsList.containsKey(uniqueId)) {
                        Method duplicateMethodInMap = methodsList.get(uniqueId);
                        logger.log(Level.SEVERE, "\u001B[31m" + "------------------DUPLICATE UNIQUE ID's are found------------------" + "\u001B[0m");
                        logger.info("\u001B[31m" + " UniqueId: " + uniqueId + "\n methods: " + duplicateMethodInMap.getName()
                                + " and " + method.getName() + "\n classes: " + duplicateMethodInMap.getDeclaringClass().getName()
                                + " and " + method.getDeclaringClass() + "\u001B[0m");
                        buildStatus = false;
                    } else {
                        methodsList.put(uniqueId, method);
                    }
                }
            }
        }
    }
}
