package com.simplifyqa.codeeditor.helper;

import com.simplifyqa.pluginbase.argument.IArgument;
import com.simplifyqa.pluginbase.common.models.Configuration;

import java.lang.reflect.Method;
import java.util.List;

public interface ICustomMethodInvoker {
    public void registerMethodsFromPackage(List<String> packageName);
    public Object invokeMethod(String methodId,String projectId, Object classObject, List<IArgument> methodArguments, Configuration configuration) throws NoSuchMethodException;
    public Method getMethod(String methodId,String projectId) throws NoSuchMethodException;
}