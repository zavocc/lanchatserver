package com.compprogroup.lan.chatapp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager {
    private final Map<String, ConnectedClient> connectedClients;

    public ClientManager() {
        this.connectedClients = new ConcurrentHashMap<>();
    }

    public synchronized boolean addClient(String username, ConnectedClient client) {
        if (connectedClients.containsKey(username)) {
            return false; // Username already taken
        }
        connectedClients.put(username, client);
        System.out.println("Client '" + username + "' connected. Total clients: " + connectedClients.size());
        return true;
    }

    public synchronized void removeClient(String username) {
        ConnectedClient removed = connectedClients.remove(username);
        if (removed != null) {
            System.out.println("Client '" + username + "' disconnected. Total clients: " + connectedClients.size());
        }
    }

    public boolean isUsernameAvailable(String username) {
        return !connectedClients.containsKey(username);
    }

    public ConnectedClient getClient(String username) {
        return connectedClients.get(username);
    }

    public Set<String> getConnectedUsernames() {
        return new HashSet<>(connectedClients.keySet());
    }

    public int getClientCount() {
        return connectedClients.size();
    }

    public void disconnectAllClients() {
        for (ConnectedClient client : connectedClients.values()) {
            client.disconnect();
        }
        connectedClients.clear();
        System.out.println("All clients disconnected");
    }

    public void broadcastMessage(String senderUsername, String message) {
        for (Map.Entry<String, ConnectedClient> entry : connectedClients.entrySet()) {
            if (!entry.getKey().equals(senderUsername)) {
                entry.getValue().sendMessage(senderUsername, message);
            }
        }
    }
}