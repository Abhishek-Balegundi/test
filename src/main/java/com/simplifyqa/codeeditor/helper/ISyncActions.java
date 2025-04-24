package com.simplifyqa.codeeditor.helper;

import com.simplifyqa.codeeditor.plugin.CodeEditorPlugin;
import com.simplifyqa.pluginbase.codeeditor.annotations.SyncAction;
import com.simplifyqa.pluginbase.codeeditor.service.ISyncTransformer;
import com.simplifyqa.pluginbase.plugin.sync.models.ActionData;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface ISyncActions {
      Logger log = Logger.getLogger(ISyncActions.class.getName());
    static List<ActionData> getActionList(List<String> packageName) {
        List<ActionData> actionDataList = new ArrayList<>();
        try {
            for (String s : packageName) {
                List<Class<?>> classes = CustomPackageScanner.getClasses(s);
                classes.forEach(clazz -> actionDataList.addAll(getAllActions(clazz)));
            }
        } catch (Exception e) {
            log.log(Level.SEVERE,"Failed to collect the actionDataList");
        }
        log.info("ActionDataList from code editor plugin: "+ actionDataList);
        return actionDataList;
    }

    private static List<ActionData> getAllActions(Class<?> clazz) {
        List<ActionData> actionDataList = new ArrayList<>();
        Method[] methods = clazz.getMethods();
        for (Method method:methods){
            if (method.isAnnotationPresent(SyncAction.class)){
                actionDataList.add(ISyncTransformer.transformToSync(method,method.getAnnotation(SyncAction.class), CodeEditorPlugin.projectId));
            }
        }
        return actionDataList;
    }
}
