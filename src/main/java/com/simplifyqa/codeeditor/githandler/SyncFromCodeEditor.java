package com.simplifyqa.codeeditor.githandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.*;

import com.simplifyqa.pluginbase.plugin.sync.models.SyncResponse;

public class SyncFromCodeEditor {
    private static final String endPoint = "http://localhost:4040/sync/codeeditor";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final int SUCCESS = 0;
    private static final int ERROR = 1;

    private static final Logger log = Logger.getLogger(SyncFromCodeEditor.class.getName());

    static {
        configureLogger();
    }

    private static void configureLogger() {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        consoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String level = record.getLevel().getLocalizedName();
                String message = record.getMessage().replaceAll("\\r?\\n", " ");
                if (level.equalsIgnoreCase("SEVERE")) {
                    return "[" + ANSI_RED + level + ANSI_RESET + "] " + message + System.lineSeparator();
                }
                return "[" + ANSI_BLUE + level + ANSI_RESET + "] " + message + System.lineSeparator();
            }
        });

        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(Level.INFO);
    }

    public static int sendRequest(String requestBody) {
        HttpURLConnection connection = null;
        try {
            printGap();
            printGap();
            log.info(ANSI_YELLOW
                    + ":::::::::::::::::::::::::::::::::::SENDING SYNC REQUEST TO AGENT:::::::::::::::::::::::::::::::::::"
                    + ANSI_RESET);

            URL url = new URL(endPoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            String responseCodeColor = responseCode == 200 ? ANSI_GREEN : ANSI_RED;
            printGap();
            printGap();
            log.info("::::::::::::::::::::::::::::::::::" + ANSI_YELLOW + " API RESPONSE CODE: " + responseCodeColor
                    + responseCode
                    + ANSI_RESET + " ::::::::::::::::::::::::::::::::::");

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            SyncResponse responseBody = parseResponse(response.toString());
            printGap();
            printGap();

            if (responseBody == null) {
                log.info("::::::::::::::::::::::::::::::::::" + ANSI_YELLOW + " NO ACTIONS TO SYNC!!" + ANSI_RESET
                        + " ::::::::::::::::::::::::::::::::::");
                return ERROR;
            }
            if (responseBody.isSaved()) {
                log.info("::::::::::::::::::::::::::::::::::" + ANSI_GREEN + " SUCCESSFULLY SYNCED ACTIONS" + ANSI_RESET
                        + " ::::::::::::::::::::::::::::::::::");
                return SUCCESS;
            }if (responseBody.isPending()) {
                log.info("::::::::::::::::::::::::::::::::::" + ANSI_YELLOW + " PREVIOUS SYNC REQUEST IS PENDING. KINDLY ACCEPT/REJECT IN UI" + ANSI_RESET
                        + " ::::::::::::::::::::::::::::::::::");
                printGap();
                log.info("Response message: "+responseBody.getErrors());
                return ERROR;
            }
            log.log(Level.SEVERE,
                    ANSI_RED + "COULD NOT AUTO SYNC: " + ANSI_YELLOW + responseBody.getErrors() + ANSI_RESET);
            return ERROR;

        } catch (Exception e) {
            printGap();
            printGap();
            log.log(Level.SEVERE, ANSI_RED + "ERROR: " + ANSI_YELLOW + e.getMessage() + ANSI_RESET);
            return ERROR;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static SyncResponse parseResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            return null;
        }

        try {
            boolean saved = jsonResponse.contains("\"saved\":true");
            boolean pending = jsonResponse.contains("\"pending\":true");
            String errors = extractField(jsonResponse, "errors");

            return new SyncResponse(pending, saved, errors);
        } catch (Exception e) {
            printGap();
            printGap();
            log.log(Level.WARNING, "Error parsing response: " + e.getMessage());
            return null;
        }
    }

    private static String extractField(String json, String fieldName) {
        int startIndex = json.indexOf("\"" + fieldName + "\":");
        if (startIndex == -1)
            return null;

        startIndex = json.indexOf(":", startIndex) + 1;
        int endIndex = json.indexOf(",", startIndex);
        if (endIndex == -1)
            endIndex = json.indexOf("}", startIndex);
        if (endIndex == -1)
            return null;

        String value = json.substring(startIndex, endIndex).trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void printGap() {
        System.out.println();
    }

    public static void main(String[] args) {
        try {
            String requestBody = System.getProperty("user.dir");
            int response = sendRequest(requestBody);
            printGap();
            printGap();
            System.exit(response);
        } catch (Exception e) {
            printGap();
            printGap();
            log.log(Level.SEVERE, ANSI_RED + "ERROR: " + ANSI_YELLOW + e.getMessage() + ANSI_RESET);
            System.exit(ERROR);
        }
    }
}