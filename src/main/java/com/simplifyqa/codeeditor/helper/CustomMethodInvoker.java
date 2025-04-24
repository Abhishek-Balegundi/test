package com.simplifyqa.codeeditor.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplifyqa.codeeditor.exceptions.MethodLoaderException;
import com.simplifyqa.codeeditor.plugin.CodeEditorPlugin;
import com.simplifyqa.pluginbase.argument.IArgument;
import com.simplifyqa.pluginbase.codeeditor.annotations.SyncAction;
import com.simplifyqa.pluginbase.common.models.Configuration;
import com.simplifyqa.pluginbase.exceptions.IncompatibleParameterTypeException;
import com.simplifyqa.pluginbase.exceptions.MethodInvocationFailedException;
import com.simplifyqa.pluginbase.exceptions.NullMethodParameterException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.reflect.Array.*;

public class CustomMethodInvoker implements ICustomMethodInvoker {
    private final Logger logger = Logger.getLogger(CustomMethodInvoker.class.getName());

    public CustomMethodInvoker() {
    }

    private final Map<String, Method> methodsList = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void registerMethodsFromPackage(List<String> packageName) {
        try {
            for (String pkg : packageName) {
                List<Class<?>> classes = CustomPackageScanner.getClasses(pkg);
                registerMethodsFromClass(classes);
            }
            logger.info("Loaded methods with IDs :" + methodsList.keySet());
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Methods couldn't be registered for package : %s , %s", packageName, e.getMessage()));
            throw new MethodLoaderException(String.format("Methods couldn't be registered for package : %s , %s", packageName, e.getMessage()));
        }
    }

    private void registerMethodsFromClass(List<Class<?>> classes) {
        for (Class eachClass : classes) {
            for (Method method : eachClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(SyncAction.class)) {
                    SyncAction annotation = method.getAnnotation((SyncAction.class));
                    String value = CodeEditorPlugin.projectId + "_" + annotation.uniqueId();
                    methodsList.put(value, method);
                }
            }
        }
    }

    @Override
    public Object invokeMethod(String methodId, String projectId, Object classObject, List<IArgument> methodArguments, Configuration configuration) throws NoSuchMethodException {
        Method method = methodsList.get(methodId);
        logger.info(String.format("Fetched Method with id : %s, name: %s ,projectId: %s", methodId, method.getName(), projectId));
        logger.info(String.format("class object : %s", classObject));

        if (Optional.ofNullable(method).isPresent()) {
            long timeout = configuration.MAX_TIME_OUT();
            if (timeout<=0){
                timeout=90000;
            }
            long endTime = System.currentTimeMillis() + timeout;

            while (System.currentTimeMillis() < endTime) {
                try {
                    if (Optional.ofNullable(methodArguments).isPresent()) {
                        Object[] args = methodArgumentGenerator(method, methodArguments);
                        Object result = method.invoke(classObject, args);

                        // Update runtime parameter if it is a variable argument
                        if (method.isVarArgs()) {
                            Object varargsArray = args[args.length - 1];
                            int lastIndex = getLength(varargsArray) - 1;
                            Object updatedValue = get(varargsArray, lastIndex);
                            methodArguments.get(methodArguments.size() - 1).updateValue(updatedValue.toString());
                            logger.info("Stored Value: " + updatedValue);
                        }
                        return result;
                    } else {
                        logger.log(Level.SEVERE, "Method Arguments set to null for method : " + methodId);
                        throw new NullMethodParameterException("Method Arguments are null.");
                    }
                } catch (InvocationTargetException ex) {
                    logger.log(Level.SEVERE, String.format("Method with Id %s could not be invoked.", methodId));
                    logger.info("Step Exception: " + ex.getCause());
                    throw new MethodInvocationFailedException(ex.getCause().getMessage());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, String.format("Method with Id %s could not be invoked.", methodId));
                    logger.info("Step Exception");
                    throw new MethodInvocationFailedException(e.getMessage());
                }
            }
            logger.log(Level.SEVERE, String.format("Method with Id %s could not be invoked within the timeout period.", methodId));
            throw new MethodInvocationFailedException("Method invocation failed after retrying for 90 seconds.");
        } else {
            logger.log(Level.SEVERE, String.format("Method with Id %s not found.", methodId));
            throw new NoSuchMethodException("Method not found.");
        }
    }


    @Override
    public Method getMethod(String methodId, String projectId) throws NoSuchMethodException {
        Method method = methodsList.get(methodId);
        if (method != null)
            return method;
        else
            throw new NoSuchMethodException("Method id doesn't matches");
    }

    private Object[] methodArgumentGenerator(Method method, List<IArgument> arguments) throws NoSuchMethodException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        //check if it is a variable arguments
        boolean hasVarargs = method.isVarArgs();
        if ((!hasVarargs && parameterTypes.length != arguments.size()) ||
                (hasVarargs && arguments.size() < parameterTypes.length - 1)) {
            logger.log(Level.SEVERE, String.format("Argument length doesn't match for method : %s ," +
                            "ActualMethodParameter Length : %s, StepParameter Length : %s",
                    method.getName(), parameterTypes.length, arguments.size()));
            throw new NoSuchMethodException("Arguments length does not match");
        }
        Object[] result = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isAssignableFrom(IArgument.class)) {
                result[i] = arguments.get(i);
                logger.info(String.format("IArgument Parameter from Method: %s, Argument type from front end: %s",
                        parameterTypes[i], arguments.get(i).getClass()));
                continue;
            }

            if (hasVarargs && i == parameterTypes.length - 1) {
                Class<?> varargsType = parameterTypes[i].getComponentType();
                int varargsLength = arguments.size() - i;
                Object varargsArray = newInstance(varargsType, varargsLength);
                for (int j = 0; j < varargsLength; j++) {
                    String value = arguments.get(i + j).getValue();
                    logger.info("Array of Values from TestData: " + value);
                    try {
                        set(varargsArray, j, objectMapper.readValue(value, varargsType));
                    } catch (JsonProcessingException e) {
                        try {
                            set(varargsArray, j, objectMapper.convertValue(value, varargsType));
                        } catch (Exception e2) {
                            throw new IncompatibleParameterTypeException(e2.getMessage());
                        }
                    }
                }
                result[i] = varargsArray;
            } else {
                String value = arguments.get(i).getValue();
                logger.info("Value from TestData: " + value);
                try {
                    result[i] = objectMapper.readValue(value, parameterTypes[i]);
                } catch (JsonProcessingException e) {
                    try {
                        result[i] = objectMapper.convertValue(value, parameterTypes[i]);
                    } catch (Exception e2) {
                        throw new IncompatibleParameterTypeException(e2.getMessage());
                    }
                }
            }
        }
        return result;
    }


}
