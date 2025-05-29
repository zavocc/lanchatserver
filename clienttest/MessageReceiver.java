import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles incoming messages from the chat server in real-time
 * Runs in a separate thread to enable concurrent message reception
 */
public class MessageReceiver implements Runnable {
    private BufferedReader input;
    private ChatClient client;
    private boolean running = true;

    public MessageReceiver(BufferedReader input, ChatClient client) {
        this.input = input;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = input.readLine()) != null) {
                // Debug: Print received message
                System.err.println("DEBUG: Received message: " + message);
                processMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("❌ Connection lost: " + e.getMessage());
            }
        }
    }

    private void processMessage(String message) {
        try {
            // Check if it's JSON or plain text
            if (message.startsWith("{") && message.endsWith("}")) {
                processJsonMessage(message);
            } else {
                // Plain text message (backward compatibility)
                client.displayMessage(message);
            }
        } catch (Exception e) {
            // If JSON parsing fails, display as plain text
            client.displayMessage(message);
        }
    }

    private void processJsonMessage(String jsonMessage) {
        try {
            // Simple JSON parsing for demo (production would use Jackson)
            if (jsonMessage.contains("\"type\":\"chat\"")) {
                processChatMessage(jsonMessage);
            } else if (jsonMessage.contains("\"type\":\"status\"")) {
                processStatusMessage(jsonMessage);
            } else if (jsonMessage.contains("\"type\":\"user\"")) {
                processUserMessage(jsonMessage);
            } else {
                // Unknown JSON type, display as-is
                client.displayMessage("📨 " + jsonMessage);
            }
        } catch (Exception e) {
            // Fallback to plain text display
            client.displayMessage(jsonMessage);
        }
    }

    private void processChatMessage(String jsonMessage) {
        try {
            // Extract username and content from JSON
            String username = extractJsonValue(jsonMessage, "username");
            String content = extractJsonValue(jsonMessage, "content");
            String timestamp = extractJsonValue(jsonMessage, "timestamp");
            
            // Format and display the message
            if (timestamp != null && !timestamp.isEmpty()) {
                // Parse ISO timestamp and format for display
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(timestamp);
                    String formattedTime = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    client.displayMessage(String.format("[%s] %s: %s", formattedTime, username, content));
                } catch (Exception e) {
                    // Fallback if timestamp parsing fails
                    client.displayMessage(String.format("%s: %s", username, content));
                }
            } else {
                client.displayMessage(String.format("%s: %s", username, content));
            }
        } catch (Exception e) {
            client.displayMessage("💬 " + jsonMessage);
        }
    }

    private void processStatusMessage(String jsonMessage) {
        try {
            String message = extractJsonValue(jsonMessage, "message");
            String level = extractJsonValue(jsonMessage, "level");
            
            String icon = "ℹ️";
            if ("ERROR".equals(level)) {
                icon = "❌";
            } else if ("WARNING".equals(level)) {
                icon = "⚠️";
            } else if ("SUCCESS".equals(level)) {
                icon = "✅";
            }
            
            client.displayMessage(String.format("%s SERVER: %s", icon, message));
        } catch (Exception e) {
            client.displayMessage("📢 " + jsonMessage);
        }
    }

    private void processUserMessage(String jsonMessage) {
        try {
            String username = extractJsonValue(jsonMessage, "username");
            String action = extractJsonValue(jsonMessage, "action");
            
            String icon = "👤";
            if ("JOIN".equals(action)) {
                icon = "👋";
                client.displayMessage(String.format("%s %s joined the chat", icon, username));
            } else if ("LEAVE".equals(action)) {
                icon = "👋";
                client.displayMessage(String.format("%s %s left the chat", icon, username));
            } else {
                client.displayMessage(String.format("%s User update: %s", icon, jsonMessage));
            }
        } catch (Exception e) {
            client.displayMessage("👥 " + jsonMessage);
        }
    }

    /**
     * Simple JSON value extraction (for demo purposes)
     * In production, use Jackson or another JSON library
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }

    public void stop() {
        running = false;
    }
}