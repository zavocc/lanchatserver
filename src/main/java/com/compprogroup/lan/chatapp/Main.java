package com.compprogroup.lan.chatapp;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    private String ipAddress;
    private int port;
    private boolean keyAuthenticated;
    private String authKey;
    private NetworkManager networkManager;
    private ClientManager clientManager;
    private ChatHistory chatHistory;

    public Main(String ipAddress, int port, boolean keyAuthenticated) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.keyAuthenticated = keyAuthenticated;
        this.clientManager = new ClientManager();
        this.chatHistory = new ChatHistory();
        this.networkManager = new NetworkManager(this, ipAddress, port);
        
        if (keyAuthenticated) {
            generateAuthKey();
        }
    }

    private void generateAuthKey() {
        // generate a random 8-character authentication key
        this.authKey = UUID.randomUUID().toString().substring(0, 8);
        System.out.println("Server Authentication Key: " + authKey);
        System.out.println("Clients must provide this key to connect.");
    }

    public void start() {
        System.out.println("Starting chat server on " + ipAddress + ":" + port);
        if (keyAuthenticated) {
            System.out.println("Key authentication enabled");
        }
        networkManager.startListening();
    }

    public void stop() {
        networkManager.stopListening();
        clientManager.disconnectAllClients();
    }

    // Getters
    public boolean isKeyAuthenticated() { return keyAuthenticated; }
    public String getAuthKey() { return authKey; }
    public ClientManager getClientManager() { return clientManager; }
    public ChatHistory getChatHistory() { return chatHistory; }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Main <ip_address> <port> [keyAuthenticated]");
            System.out.println("Example: java Main 192.168.1.100 8080 keyAuthenticated");
            return;
        }

        String ipAddress = args[0];
        int port;
        boolean keyAuthenticated = false;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            return;
        }

        if (args.length > 2 && "keyAuthenticated".equals(args[2])) {
            keyAuthenticated = true;
        }

        Main server = new Main(ipAddress, port, keyAuthenticated);
        
        // Add shutdown hook to gracefully close server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down server...");
            server.stop();
        }));

        server.start();
    }
}