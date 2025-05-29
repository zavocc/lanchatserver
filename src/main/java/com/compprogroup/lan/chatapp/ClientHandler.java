package com.compprogroup.lan.chatapp;

import java.io.*;
import java.net.*;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ClientHandler implements Runnable {
    private Main server;
    private Socket clientSocket;
    private ConnectedClient connectedClient;
    private String username;

    public ClientHandler(Main server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);

            // Handle authentication if required
            if (server.isKeyAuthenticated()) {
                sendMessage(output, new AuthMessage("REQUIRED"));
                String authResponse = input.readLine();
                if (!server.getAuthKey().equals(authResponse)) {
                    sendMessage(output, new AuthMessage("FAILED"));
                    clientSocket.close();
                    return;
                }
                sendMessage(output, new AuthMessage("SUCCESS"));
            }

            // Handle username registration
            sendMessage(output, new UserMessage("REQUIRED"));
            String usernameRequest = input.readLine();
            
            if (usernameRequest == null || usernameRequest.trim().isEmpty()) {
                sendMessage(output, new UserMessage("INVALID"));
                clientSocket.close();
                return;
            }

            username = usernameRequest.trim();
            
            // Validate username (alphanumeric and underscore only, 3-20 characters)
            if (!isValidUsername(username)) {
                sendMessage(output, new UserMessage("INVALID"));
                clientSocket.close();
                return;
            }

            // Check if username is available
            if (!server.getClientManager().isUsernameAvailable(username)) {
                sendMessage(output, new UserMessage("TAKEN"));
                clientSocket.close();
                return;
            }

            // Create connected client and add to manager
            connectedClient = new ConnectedClient(username, clientSocket);
            if (server.getClientManager().addClient(username, connectedClient)) {
                sendMessage(output, new UserMessage("SUCCESS", username));
                sendStatusMessage("Welcome to the chat server! You are connected as '" + username + "'");
                
                // Send recent chat history
                sendRecentHistory();
                
                // Listen for messages
                listenForMessages(input);
            } else {
                sendMessage(output, new StatusMessage("Connection failed", "ERROR"));
                clientSocket.close();
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private boolean isValidUsername(String username) {
        // Username must be 3-20 characters, alphanumeric and underscore only
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
        return pattern.matcher(username).matches();
    }

    private void sendRecentHistory() {
        var recentMessages = server.getChatHistory().getRecentMessages(10);
        if (!recentMessages.isEmpty()) {
            sendStatusMessage("Recent messages:");
            for (var message : recentMessages) {
                // Convert ChatHistory.ChatMessage to our new ChatMessage format
                ChatMessage chatMsg = new ChatMessage(
                    message.getUsername(), 
                    message.getContent(), 
                    message.getTimestamp()
                );
                sendMessage(connectedClient.getOutput(), chatMsg);
            }
        }
    }

    private void listenForMessages(BufferedReader input) throws IOException {
        String inputLine;
        while ((inputLine = input.readLine()) != null) {
            if (inputLine.trim().isEmpty()) {
                continue;
            }

            // Check if it's a JSON message format
            if (JsonUtil.isValidJson(inputLine)) {
                handleJsonMessage(inputLine);
            } else {
                // Treat as plain text message for backward compatibility
                handlePlainMessage(inputLine);
            }
        }
    }

    private void handleJsonMessage(String jsonMessage) {
        try {
            Message message = JsonUtil.fromJson(jsonMessage);
            
            if (message instanceof ChatMessage) {
                ChatMessage chatMsg = (ChatMessage) message;
                
                // Verify the username matches the connected client
                if (!username.equals(chatMsg.getUsername())) {
                    sendStatusMessage("Error: Username in message doesn't match your connection", "ERROR");
                    return;
                }
                
                processMessage(chatMsg.getContent());
            } else {
                sendStatusMessage("Error: Expected chat message type", "WARNING");
            }
        } catch (JsonProcessingException e) {
            sendStatusMessage("Error: Failed to parse JSON message - " + e.getMessage(), "ERROR");
        }
    }

    private void handlePlainMessage(String message) {
        processMessage(message);
    }

    private void processMessage(String message) {
        if (message.trim().isEmpty()) {
            return;
        }

        System.out.println("DEBUG: Processing message from '" + username + "': " + message);

        // Add to chat history
        server.getChatHistory().addMessage(username, message);
        
        // Broadcast to all other clients as JSON
        ChatMessage chatMsg = new ChatMessage(username, message);
        System.out.println("DEBUG: Created ChatMessage, now broadcasting...");
        server.getClientManager().broadcastJsonMessage(chatMsg);
    }

    private void sendMessage(PrintWriter output, Message message) {
        try {
            String json = JsonUtil.toJson(message);
            output.println(json);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing message: " + e.getMessage());
            output.println("{\"type\":\"status\",\"message\":\"Error sending message\",\"level\":\"ERROR\"}");
        }
    }

    private void sendStatusMessage(String message) {
        sendStatusMessage(message, "INFO");
    }

    private void sendStatusMessage(String message, String level) {
        if (connectedClient != null) {
            sendMessage(connectedClient.getOutput(), new StatusMessage(message, level));
        }
    }

    private void cleanup() {
        if (username != null) {
            server.getClientManager().removeClient(username);
        }
        if (connectedClient != null) {
            connectedClient.disconnect();
        }
    }
}