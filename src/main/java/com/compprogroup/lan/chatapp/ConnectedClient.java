package com.compprogroup.lan.chatapp;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ConnectedClient {
    private String username;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private LocalDateTime connectionTime;

    public ConnectedClient(String username, Socket socket) throws IOException {
        this.username = username;
        this.socket = socket;
        this.connectionTime = LocalDateTime.now();
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.output = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }

    public BufferedReader getInput() {
        return input;
    }

    public PrintWriter getOutput() {
        return output;
    }

    public LocalDateTime getConnectionTime() {
        return connectionTime;
    }

    public void sendMessage(String senderUsername, String message) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String formattedMessage = String.format("[%s] %s: %s", timestamp, senderUsername, message);
            output.println(formattedMessage);
        } catch (Exception e) {
            System.err.println("Error sending message to " + username + ": " + e.getMessage());
        }
    }

    public void sendStatusMessage(String message) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String formattedMessage = String.format("[%s] SERVER: %s", timestamp, message);
            output.println(formattedMessage);
        } catch (Exception e) {
            System.err.println("Error sending status message to " + username + ": " + e.getMessage());
        }
    }

    /**
     * Send a pre-serialized JSON message to the client
     */
    public void sendJsonMessage(String jsonMessage) {
        try {
            output.println(jsonMessage);
        } catch (Exception e) {
            System.err.println("Error sending JSON message to " + username + ": " + e.getMessage());
        }
    }

    /**
     * Send a Message object as JSON to the client
     */
    public void sendJsonMessage(Message message) {
        try {
            String json = JsonUtil.toJson(message);
            output.println(json);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing message for " + username + ": " + e.getMessage());
            // Fallback to plain text error message
            sendStatusMessage("Error: Failed to send message");
        }
    }

    public void disconnect() {
        try {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error disconnecting client " + username + ": " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}