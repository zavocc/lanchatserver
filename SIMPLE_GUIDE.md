# Simple Guide to Connect to Chat Server

## Connecting to server

### 1. Basic Connection
```java
import java.io.*;
import java.net.*;

// Connect to server
Socket socket = new Socket("localhost", 8080);
BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
```

### 2. Handle Authentication (if enabled)
```java
// Server will ask for auth if required
String authMessage = input.readLine();
if (authMessage.contains("REQUIRED")) {
    output.println("your-auth-key"); // Send your auth key
    String authResult = input.readLine();
    if (!authResult.contains("SUCCESS")) {
        System.out.println("Authentication failed!");
        return;
    }
}
```

### 3. Register Username
```java
// Server will ask for username
String userMessage = input.readLine();
if (userMessage.contains("REQUIRED")) {
    output.println("your_username"); // Send your username
    String userResult = input.readLine();
    if (userResult.contains("SUCCESS")) {
        System.out.println("Connected successfully!");
    } else if (userResult.contains("TAKEN")) {
        System.out.println("Username already taken!");
        return;
    }
}
```

---

## Manual Chat History Fetching

### Option 1: Using the `/sync` Command (Recommended)
The easiest way to manually fetch chat history is using the built-in `/sync` command:

```java
// Send sync command as JSON message
String syncRequest = "{\"type\":\"chat\",\"username\":\"" + username + "\",\"content\":\"/sync\"}";
output.println(syncRequest);

// Server will respond with recent chat history
// Listen for incoming messages to receive the history
```

### Option 2: Send Plain Text Sync Command
For backward compatibility, you can also send the sync command as plain text:

```java
// Send sync command directly
output.println("/sync");

// Server will respond with recent chat history
```

### Option 3: Custom History Request Implementation
If you want to implement a custom history request system, here's how you could extend it:

```java
// Custom JSON request for specific number of messages
String customHistoryRequest = String.format(
    "{\"type\":\"history\",\"username\":\"%s\",\"count\":%d}", 
    username, 20  // Request last 20 messages
);
output.println(customHistoryRequest);
```

**Note:** The custom history request would require server-side modifications to handle the "history" message type.

### Manual History Fetch Example
```java
public class HistoryFetcher {
    private PrintWriter output;
    private String username;
    
    public HistoryFetcher(PrintWriter output, String username) {
        this.output = output;
        this.username = username;
    }
    
    // Method to manually request chat history
    public void requestHistory() {
        System.out.println("🔄 Requesting chat history...");
        String syncRequest = String.format(
            "{\"type\":\"chat\",\"username\":\"%s\",\"content\":\"/sync\"}", 
            username
        );
        output.println(syncRequest);
    }
    
    // Method to request history on a timer (auto-refresh)
    public void startAutoSync(int intervalSeconds) {
        Timer timer = new Timer(true); // Daemon thread
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                requestHistory();
            }
        }, 0, intervalSeconds * 1000);
    }
}
```

### Using History Fetcher
```java
// After connecting and registering username
HistoryFetcher historyFetcher = new HistoryFetcher(output, username);

// Manual sync
historyFetcher.requestHistory();

// Auto-sync every 30 seconds (optional)
// historyFetcher.startAutoSync(30);
```

---

## Fetching real-time message

### Automatic History Fetch
**The server automatically sends recent chat history when you connect!**
- Server sends the **last 10 messages** automatically upon connection
- No manual request needed for initial history

### Real-Time Message Listener
```java
// Create a thread to listen for incoming messages
class MessageListener implements Runnable {
    private BufferedReader input;
    private boolean running = true;
    
    public MessageListener(BufferedReader input) {
        this.input = input;
    }
    
    @Override
    public void run() {
        try {
            String message;
            while (running && (message = input.readLine()) != null) {
                System.out.println("Received: " + message);
                // Messages come as JSON or plain text
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + e.getMessage());
        }
    }
    
    private void handleMessage(String message) {
        // Check if it's a JSON message
        if (message.startsWith("{")) {
            // Parse JSON (chat messages, status updates)
            if (message.contains("\"type\":\"chat\"")) {
                System.out.println("💬 Chat: " + message);
            } else if (message.contains("\"type\":\"status\"")) {
                System.out.println("ℹ️ Status: " + message);
                
                // Check if this is a response to sync command
                if (message.contains("Recent messages:")) {
                    System.out.println("📜 History sync completed!");
                }
            }
        } else {
            // Plain text message
            System.out.println("📝 Message: " + message);
        }
    }
    
    public void stop() {
        running = false;
    }
}
```

### Start Listening
```java
// Start the message listener thread
MessageListener listener = new MessageListener(input);
Thread listenerThread = new Thread(listener);
listenerThread.start();

System.out.println("✅ Now receiving messages in real-time!");
```

---

## Complete Working Example with Manual History

```java
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class AdvancedClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    
    public static void main(String[] args) {
        new AdvancedClient().start();
    }
    
    public void start() {
        try {
            // 1. Connect to server
            socket = new Socket("localhost", 8080);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            
            System.out.println("🔗 Connected to server!");
            
            // 2. Handle authentication (if needed)
            String authMessage = input.readLine();
            if (authMessage.contains("REQUIRED")) {
                output.println(""); // No auth key for this example
                String authResult = input.readLine();
                if (!authResult.contains("SUCCESS")) {
                    System.out.println("❌ Authentication failed!");
                    return;
                }
            }
            
            // 3. Register username
            String userMessage = input.readLine();
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your username: ");
            username = scanner.nextLine();
            
            output.println(username);
            String userResult = input.readLine();
            
            if (userResult.contains("SUCCESS")) {
                System.out.println("✅ Welcome " + username + "!");
            } else {
                System.out.println("❌ Username registration failed!");
                return;
            }
            
            // 4. Start receiving messages (including automatic history)
            Thread messageThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = input.readLine()) != null) {
                        System.out.println("📨 " + message);
                    }
                } catch (IOException e) {
                    System.out.println("🔌 Connection lost!");
                }
            });
            messageThread.start();
            
            // 5. Interactive command handling
            System.out.println("💬 Commands available:");
            System.out.println("  /sync - Manually fetch chat history");
            System.out.println("  /clear - Clear screen");
            System.out.println("  /quit - Exit");
            System.out.println("  Or type any message to send to chat");
            
            String userInput;
            while (!(userInput = scanner.nextLine()).equals("/quit")) {
                if (userInput.equals("/sync")) {
                    requestHistory();
                } else if (userInput.equals("/clear")) {
                    clearScreen();
                } else if (!userInput.trim().isEmpty()) {
                    sendMessage(userInput);
                }
            }
            
            socket.close();
            
        } catch (IOException e) {
            System.out.println("❌ Connection error: " + e.getMessage());
        }
    }
    
    private void requestHistory() {
        System.out.println("🔄 Requesting chat history...");
        String syncRequest = String.format(
            "{\"type\":\"chat\",\"username\":\"%s\",\"content\":\"/sync\"}", 
            username
        );
        output.println(syncRequest);
    }
    
    private void sendMessage(String message) {
        String jsonMessage = String.format(
            "{\"type\":\"chat\",\"username\":\"%s\",\"content\":\"%s\"}", 
            username, 
            message.replace("\"", "\\\"")
        );
        output.println(jsonMessage);
    }
    
    private void clearScreen() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
        System.out.println("=== Screen cleared ===");
    }
}
```

---

## ⚡ What Happens Automatically vs Manually

### Automatic (When You Connect):
1. **Authentication** (if server requires it)
2. **Username Registration** 
3. **Automatic History Fetch** - Server sends last 10 messages
4. **Real-time Updates** - All new messages appear instantly

### Manual History Fetching:
1. **`/sync` command** - Request recent messages anytime
2. **Custom requests** - Implement your own history fetching logic
3. **Periodic sync** - Set up automatic periodic history refreshing
4. **On-demand** - Fetch history when needed (e.g., after reconnection)

### Message Flow:
```
Client connects → Server sends history → Real-time messages begin
   ↓                     ↓                       ↓
[Auth]              [Last 10 msgs]         [Live messages]
   ↓                     ↓                       ↓
Manual /sync → Server sends recent history → Continue real-time
```

---

## 🎯 Quick Test

1. **Start the server:**
   ```bash
   cd /home/marcusz/lanchatapp
   mvn exec:java -Dexec.mainClass="com.compprogroup.lan.chatapp.Main" -Dexec.args="localhost 8080"
   ```

2. **Use the existing test client (has /sync command):**
   ```bash
   cd /home/marcusz/lanchatapp/clienttest
   ./run_client.sh
   ```
   Then type `/sync` to manually fetch history

3. **Or compile and run the advanced example:**
   ```bash
   javac AdvancedClient.java
   java AdvancedClient
   ```

---

## 📝 Key Points for Manual History Fetching

- **`/sync` command** - Built-in way to manually request recent history
- **JSON format** - Send sync requests as JSON for best compatibility
- **Server responds** - History comes back through the normal message stream
- **No special handling** - History messages look like regular chat messages
- **Rate limiting** - Don't spam sync requests; be reasonable with frequency
- **Automatic + Manual** - You get initial history automatically, manual sync for updates
- **Thread-safe** - Can request history from any thread safely

**Pro tip:** Use manual history fetching after network interruptions or when you suspect you missed messages! 🔄