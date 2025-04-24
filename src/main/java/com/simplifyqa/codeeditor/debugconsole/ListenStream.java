package com.simplifyqa.codeeditor.debugconsole;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ListenStream {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 4041;

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the server. Listening for logs...");

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Make sure that host is running and you have authority to connect.");
        }
    }
}

