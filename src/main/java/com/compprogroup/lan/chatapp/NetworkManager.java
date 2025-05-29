package com.compprogroup.lan.chatapp;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class NetworkManager {
    private ServerSocket serverSocket;
    private Main server;
    private boolean isListening;
    private String ipAddress;
    private int port;
    private ExecutorService clientThreadPool;

    public NetworkManager(Main server, String ipAddress, int port) {
        this.server = server;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isListening = false;
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    public void startListening() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ipAddress, port));
            isListening = true;
            
            System.out.println("Server listening on " + ipAddress + ":" + port);
            
            while (isListening) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connection from: " + clientSocket.getRemoteSocketAddress());
                    
                    // Handle each client in a separate thread
                    clientThreadPool.submit(new ClientHandler(server, clientSocket));
                    
                } catch (IOException e) {
                    if (isListening) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    public void stopListening() {
        isListening = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            clientThreadPool.shutdown();
            if (!clientThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                clientThreadPool.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    public boolean isListening() {
        return isListening;
    }
}