package org.example.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class ChessServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started. Waiting for client on port " + port);
            clientSocket = serverSocket.accept(); // This will block until a client connects
            System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            // Initialize output and input streams
            // IMPORTANT: Initialize ObjectOutputStream first on both client and server
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // Start a new thread to listen for incoming messages from the client
            new Thread(new ClientHandler(in)).start();

            // TODO: You'll need to integrate this with your game logic
            // e.g., disable local input for the opponent's color until a move is received

        } catch (IOException e) {
            System.err.println("Error starting server or accepting client: " + e.getMessage());
            // Handle error (e.g., show an error message in the GUI)
        }
    }

    public void sendMove(Object moveData) { // moveData could be a custom class
        try {
            if (out != null) {
                out.writeObject(moveData);
                out.flush(); // Ensure data is sent immediately
            }
        } catch (IOException e) {
            System.err.println("Error sending move: " + e.getMessage());
        }
    }

    // Method to close connections (call this when the game ends or server stops)
    public void stop() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server resources: " + e.getMessage());
        }
    }

    // Inner class to handle messages from the client
    private class ClientHandler implements Runnable {
        private ObjectInputStream clientIn;

        public ClientHandler(ObjectInputStream in) {
            this.clientIn = in;
        }

        @Override
        public void run() {
            try {
                Object receivedData;
                while ((receivedData = clientIn.readObject()) != null) {
                    // TODO: Process the received move data from the client
                    // This will likely involve:
                    // 1. Deserializing receivedData to your Move object/data structure
                    // 2. Validating the move
                    // 3. Updating your local game board
                    // 4. Refreshing the GUI
                    // 5. Enabling local input for your turn
                    System.out.println("Server received move: " + receivedData);
                    // Example: ((ChessGame) yourGameInstance).processOpponentMove(receivedData);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error receiving data from client or client disconnected: " + e.getMessage());
                // Handle client disconnection (e.g., show a message, end the game)
            } finally {
                // Clean up when client disconnects
                stop();
            }
        }
    }
}
