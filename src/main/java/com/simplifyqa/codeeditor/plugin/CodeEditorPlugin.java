package com.simplifyqa.codeeditor.plugin;

import com.simplifyqa.codeeditor.helper.CustomMethodInvoker;
import com.simplifyqa.codeeditor.helper.ICustomMethodInvoker;
import com.simplifyqa.codeeditor.helper.ISyncActions;
import com.simplifyqa.pluginbase.codeeditor.model.CodeEditorSPI;
import com.simplifyqa.pluginbase.codeeditor.model.PluginType;
import com.simplifyqa.pluginbase.codeeditor.service.IAutoInjector;
import com.simplifyqa.pluginbase.common.models.AutomationInfo;
import com.simplifyqa.pluginbase.common.models.Configuration;
import com.simplifyqa.pluginbase.common.models.web.networklogs.NetworkLogs;
import com.simplifyqa.pluginbase.common.models.web.networklogs.NetworkLogsWrapper;
import com.simplifyqa.pluginbase.plugin.drivers.QADriver;
import com.simplifyqa.pluginbase.plugin.execution.models.pluginstep.ExecutionStep;
import com.simplifyqa.pluginbase.plugin.execution.models.pluginstep.PluginNormalStep;
import com.simplifyqa.pluginbase.plugin.execution.models.response.ExecutionResponse;
import com.simplifyqa.pluginbase.plugin.execution.models.response.PluginNormalStepResponseData;
import com.simplifyqa.pluginbase.plugin.execution.models.response.PluginResponseData;
import com.simplifyqa.pluginbase.plugin.sync.models.ActionData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CodeEditorPlugin implements CodeEditorSPI {
    private QADriver driver;
    private Configuration configuration;
    public static final String projectId="1";
    private static final Logger log = Logger.getLogger(CodeEditorPlugin.class.getName());
    private static final ICustomMethodInvoker methodInvoker;
    private final Map<Class<?>, Object> classObjects;

    static {
        log.info("loading all methods from Custom Plugin with project id: " + projectId);
        methodInvoker = new CustomMethodInvoker();
        methodInvoker.registerMethodsFromPackage(List.of("com.simplifyqa.codeeditor"));
    }

    public CodeEditorPlugin() {
        classObjects = new HashMap<>();
    }

    @Override
    public void close() {
        classObjects.clear();
        this.driver = null;
    }

    @Override
    public PluginType getPluginType() {
        return PluginType.CUSTOM;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public void setConfiguration(AutomationInfo automationInfo) {
        this.configuration = automationInfo.configuration();
    }

    @Override
    public QADriver getInstance() {
        return driver;
    }

    @Override
    public void setInstance(QADriver qaDriver) {
        this.driver = qaDriver;
    }

    public <T extends ExecutionStep> ExecutionResponse execute(T step) {
        log.info("step received by code editor execution plugin ");
        ExecutionResponse response = new ExecutionResponse();
        PluginNormalStep actualStep = null;
        Method method = null;
        Instant startTime = Instant.now();
        try {
            actualStep = (PluginNormalStep) step;
            log.info("Method's Unique Id from Action: " + actualStep.action().uniqueId());
            method = methodInvoker.getMethod(actualStep.action().uniqueId(), getProjectId());
            boolean stepStatus = findAndTriggerMethod(method, actualStep);
            response.setStepStatus(stepStatus);
            log.info("Plugin step completed");
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.info("Method could not triggered : " + e.getMessage());
            response.setStepStatus(false);
            response.setInformativeException(e);
        } catch (NoSuchMethodException e) {
            log.info("Method not found.");
            response.setStepStatus(false);
            response.setInformativeException(e);
        } catch (Exception e) {
            log.info(e.getMessage());
            response.setStepStatus(false);
            response.setInformativeException(e);
        }

        PluginNormalStepResponseData pluginNormalStepResponseData = new PluginNormalStepResponseData();
        try {
            checkAndAddScreenshotAndNetworkLogs(pluginNormalStepResponseData, startTime, response.isStepStatus(), actualStep != null && actualStep.takeScreenShot());
        } catch (Exception e) {
            response.setStepStatus(false);
            if (response.getInformativeException() == null)
                response.setInformativeException(e);
        }
        PluginResponseData pluginResponseData = PluginResponseData.builder()
                .pluginNormalStepResponseData(pluginNormalStepResponseData)
                .build();
        response.setPluginResponseData(pluginResponseData);
        return response;
    }

    protected boolean findAndTriggerMethod(Method method, PluginNormalStep step)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (!classObjects.containsKey(method.getDeclaringClass())) {
            Object newInstance = method.getDeclaringClass().getConstructor().newInstance();
            classObjects.put(method.getDeclaringClass(), newInstance);
        }
        IAutoInjector.autoInjectDriver(classObjects.get(method.getDeclaringClass()),driver);
        IAutoInjector.autoInjectCurrentObject(classObjects.get(method.getDeclaringClass()),step.sqaObject());
        return (boolean) methodInvoker.invokeMethod(step.action().uniqueId(), getProjectId(), classObjects.get(method.getDeclaringClass()), step.parameters(), configuration);
    }

    private void checkAndAddScreenshotAndNetworkLogs(PluginNormalStepResponseData pluginNormalStepResponseData, Instant startTime, boolean stepStatus, boolean takeScreenshot) {
        NetworkLogsWrapper networkLogsWrapper = driver.getNetworkLogsWrapper();
        if (networkLogsWrapper != null) {
            pluginNormalStepResponseData.setNetworkLogsWrapper(setResponseTimeAndGetNetworkLogs(startTime, networkLogsWrapper));
        }
        if (!stepStatus || takeScreenshot) {
            String base64Screenshot = driver.captureScreenshot();
            pluginNormalStepResponseData.setScreenShot(base64Screenshot);
        }
    }

    private NetworkLogsWrapper setResponseTimeAndGetNetworkLogs(Instant startTime, NetworkLogsWrapper wrapper) {
        List<NetworkLogs> networkLogs = wrapper.getNetworkLogs();
        if (networkLogs.isEmpty()) return wrapper;
        networkLogs.forEach(l -> {
            if (l == null || l.getParams() == null) return;
            long epochTime = l.getParams().getTimestamp();
            long timestamp = Duration.between(startTime, Instant.ofEpochMilli(l.getParams().getTimestamp())).toMillis();
            l.getParams().setTimestamp(timestamp > 0 ? timestamp : 0);
            l.getParams().setLocalDateTimeStamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochTime), ZoneId.systemDefault()).toString());
        });
        return wrapper;
    }

    @Override
    public List<ActionData> sync() {
        try {
            return ISyncActions.getActionList(List.of("com.simplifyqa.codeeditor"));
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to sync custom plugin actions: " + e.getMessage());
            return null;
        }
    }
}
