## Simple LAN Chat Server
Project for Computer Programming 2.

Please see [Simple Guide](./SIMPLE_GUIDE.md) for client reference

This server is a standalone program where you run this as a server which stores username and its chat history onto memory to power simple chat functionality to your apps on the network.

## Why is this system library built?
Apps like Twitch, Minecraft, Roblox or other multiplayer games, or YouTube live chat feature have real-time chat functionality to connect to users. But writing your own from scratch is tedious as you need to design and implement live connection and fetching.

This server library aims to make things easier by providing the infrastructure and chat functionality to your apps. Under the hood this uses standard Java.net class and JSON for data exchange.

The library is thread-safe, and can accept concurrent connections, with support for optional invitation key for simple authentication.

## How to run
```shell
mvn compile

# Start server without authentication
mvn exec:java -Dexec.mainClass="com.compprogroup.lan.chatapp.Main" -Dexec.args="localhost 8080"

# Start server with authentication  
mvn exec:java -Dexec.mainClass="com.compprogroup.lan.chatapp.Main" -Dexec.args="localhost 8080 keyAuthenticated"
```

## Code structure
in `src/main/java/com.compproggroup/lan/chatapp` we have 8 components

1. `Main.java` - Entrypoint where the server object is being created and started, with optional UUID-based key authentication as an argument
2. `ChatHistory.java` - Methods and main central object where chat history is being stored so client can sync later, it also contains clear and get methods
3. `ClientManager.java` - Similar to chat history where it stores chat history data, this also stores connected clients based on username, usernames are unique here... Methods include managing connections, connected clients, getting size of connections, and broadcasting message
4. `ConnectedClient.java` - This is now focused on the individual client itself, with methods include getting the username, sockets, input and output messages, connection times, and sending messages. It can also, return messages as JSON so that it's to easy to parse
5. `Message.java` - This logs user and system messages. Including authentication and connection status
6. `NetworkManager.java` - handles networking for opening ports, sockets.