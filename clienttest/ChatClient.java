import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Simple chat client for testing the chat server
 * Supports authentication, real-time messaging, and slash commands
 */
public class ChatClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Scanner scanner;
    private String username;
    private boolean connected = false;
    private MessageReceiver messageReceiver;

    public ChatClient() {
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }

    public void start() {
        System.out.println("=== Chat Client Test Application ===");
        System.out.println("This client tests the chat server with JSON messaging support");
        System.out.println();

        try {
            // Get connection details from user
            String serverAddress = getServerAddress();
            int port = getPort();
            String authKey = getAuthKey();
            String username = getUsername();

            // Connect to server
            if (connectToServer(serverAddress, port)) {
                // Handle authentication if needed
                if (handleAuthentication(authKey)) {
                    // Handle username registration
                    if (handleUsernameRegistration(username)) {
                        System.out.println("\n✅ Successfully connected to chat server!");
                        System.out.println("Available commands:");
                        System.out.println("  /clear - Clear the chat screen (client-only)");
                        System.out.println("  /sync - Manually sync chat history");
                        System.out.println("  /quit - Disconnect from server");
                        System.out.println("Type your message and press Enter to send.\n");

                        // Start message receiver thread
                        startMessageReceiver();

                        // Handle user input
                        handleUserInput();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private String getServerAddress() {
        System.out.print("Enter server IP address (default: localhost): ");
        String address = scanner.nextLine().trim();
        return address.isEmpty() ? "localhost" : address;
    }

    private int getPort() {
        System.out.print("Enter server port (default: 8080): ");
        String portStr = scanner.nextLine().trim();
        try {
            return portStr.isEmpty() ? 8080 : Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port, using default 8080");
            return 8080;
        }
    }

    private String getAuthKey() {
        System.out.print("Enter authentication key (optional, press Enter to skip): ");
        return scanner.nextLine().trim();
    }

    private String getUsername() {
        String username;
        do {
            System.out.print("Enter your username (3-20 characters, alphanumeric + underscore): ");
            username = scanner.nextLine().trim();
            if (username.length() < 3 || username.length() > 20) {
                System.out.println("❌ Username must be 3-20 characters long!");
                continue;
            }
            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                System.out.println("❌ Username can only contain letters, numbers, and underscores!");
                continue;
            }
            break;
        } while (true);
        return username;
    }

    private boolean connectToServer(String address, int port) {
        try {
            System.out.println("\n🔌 Connecting to " + address + ":" + port + "...");
            socket = new Socket(address, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            System.out.println("✅ Connected to server!");
            return true;
        } catch (IOException e) {
            System.err.println("❌ Failed to connect to server: " + e.getMessage());
            return false;
        }
    }

    private boolean handleAuthentication(String authKey) throws IOException {
        String response = input.readLine();
        
        if (response == null) {
            System.err.println("❌ Server disconnected unexpectedly");
            return false;
        }

        // Check if it's JSON or plain text
        if (response.startsWith("{")) {
            // JSON response - parse it
            if (response.contains("\"type\":\"auth\"") && response.contains("\"action\":\"REQUIRED\"")) {
                System.out.println("🔐 Server requires authentication");
                if (authKey.isEmpty()) {
                    System.err.println("❌ Authentication required but no key provided!");
                    return false;
                }
                
                // Send auth key
                output.println(authKey);
                
                // Wait for auth result
                String authResult = input.readLine();
                if (authResult.contains("\"action\":\"SUCCESS\"")) {
                    System.out.println("✅ Authentication successful!");
                    return true;
                } else {
                    System.err.println("❌ Authentication failed!");
                    return false;
                }
            }
        } else {
            // Plain text response (backward compatibility)
            if (response.equals("AUTH_REQUIRED")) {
                System.out.println("🔐 Server requires authentication");
                if (authKey.isEmpty()) {
                    System.err.println("❌ Authentication required but no key provided!");
                    return false;
                }
                
                output.println(authKey);
                String authResult = input.readLine();
                if (authResult.equals("AUTH_SUCCESS")) {
                    System.out.println("✅ Authentication successful!");
                    return true;
                } else {
                    System.err.println("❌ Authentication failed!");
                    return false;
                }
            }
        }
        
        return true; // No auth required
    }

    private boolean handleUsernameRegistration(String username) throws IOException {
        this.username = username;
        
        String response = input.readLine();
        if (response == null) {
            System.err.println("❌ Server disconnected unexpectedly");
            return false;
        }

        // Handle username request (JSON or plain text)
        if (response.contains("USERNAME_REQUIRED") || 
            (response.contains("\"type\":\"user\"") && response.contains("\"action\":\"REQUIRED\""))) {
            
            // Send username
            output.println(username);
            
            // Wait for response
            String result = input.readLine();
            if (result == null) {
                System.err.println("❌ Server disconnected unexpectedly");
                return false;
            }

            if (result.contains("SUCCESS") || result.contains("CONNECTION_SUCCESS")) {
                return true;
            } else if (result.contains("TAKEN") || result.contains("USERNAME_TAKEN")) {
                System.err.println("❌ Username '" + username + "' is already taken!");
                return false;
            } else if (result.contains("INVALID") || result.contains("USERNAME_INVALID")) {
                System.err.println("❌ Username '" + username + "' is invalid!");
                return false;
            } else {
                System.err.println("❌ Username registration failed: " + result);
                return false;
            }
        }
        
        return true;
    }

    private void startMessageReceiver() {
        messageReceiver = new MessageReceiver(input, this);
        Thread receiverThread = new Thread(messageReceiver);
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void handleUserInput() {
        while (connected && socket.isConnected()) {
            String userInput = scanner.nextLine();
            
            if (userInput == null) {
                break;
            }

            userInput = userInput.trim();
            
            if (userInput.isEmpty()) {
                continue;
            }

            // Handle slash commands
            if (userInput.startsWith("/")) {
                handleSlashCommand(userInput);
            } else {
                // Send regular message
                sendMessage(userInput);
            }
        }
    }

    private void handleSlashCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/clear":
                clearScreen();
                break;
            case "/sync":
                requestChatHistory();
                break;
            case "/quit":
            case "/exit":
                System.out.println("👋 Disconnecting from server...");
                connected = false;
                break;
            case "/help":
                showHelp();
                break;
            default:
                System.out.println("❌ Unknown command: " + cmd);
                System.out.println("Type /help for available commands");
        }
    }

    private void clearScreen() {
        // Clear screen using ANSI escape codes
        System.out.print("\033[2J\033[H");
        System.out.flush();
        System.out.println("=== Chat cleared (client-only) ===");
    }

    private void requestChatHistory() {
        System.out.println("🔄 Requesting chat history sync...");
        // Send a JSON message requesting history sync
        String syncRequest = "{\"type\":\"chat\",\"username\":\"" + username + "\",\"content\":\"/sync\"}";
        output.println(syncRequest);
    }

    private void showHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("/clear  - Clear the chat screen (client-only)");
        System.out.println("/sync   - Manually sync chat history");
        System.out.println("/help   - Show this help message");
        System.out.println("/quit   - Disconnect from server");
        System.out.println("========================\n");
    }

    private void sendMessage(String message) {
        try {
            // Send as JSON message
            String jsonMessage = String.format(
                "{\"type\":\"chat\",\"username\":\"%s\",\"content\":\"%s\"}", 
                username, 
                message.replace("\"", "\\\"")
            );
            output.println(jsonMessage);
        } catch (Exception e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (messageReceiver != null) {
                messageReceiver.stop();
            }
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
            System.err.println("Error disconnecting: " + e.getMessage());
        }
        System.out.println("👋 Disconnected from server");
    }

    // Method for MessageReceiver to call when displaying messages
    public void displayMessage(String message) {
        System.out.println(message);
    }
}