package org.example.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.IOException;

public class ChessClient {
    private Socket socket;
    private ObjectOutputStream out; // For sending data
    private ObjectInputStream in;  // For receiving data
    private String serverAddress;
    private int serverPort;

    public ChessClient(String address, int port) {
        this.serverAddress = address;
        this.serverPort = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server: " + serverAddress);

            // Initialize output and input streams
            // IMPORTANT: Initialize ObjectOutputStream first on both client and server
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Start a new thread to listen for incoming messages from the server
            new Thread(new ServerHandler(in)).start();

            // You'll need to integrate this with your game logic
            // e.g., disable local input if it's not your turn

            return true;
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            // Handle error (e.g., show an error message in the GUI)
            return false;
        }
    }

    public void sendMove(Object moveData) { // moveData could be your custom class
        try {
            if (out != null) {
                out.writeObject(moveData);
                out.flush(); // Ensure data is sent immediately
            }
        } catch (IOException e) {
            System.err.println("Error sending move: " + e.getMessage());
        }
    }

    // Method to close connections
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error disconnecting from server: " + e.getMessage());
        }
    }
    // Inner class to handle messages from the server
    private class ServerHandler implements Runnable {
        private ObjectInputStream serverIn;

        public ServerHandler(ObjectInputStream in) {
            this.serverIn = in;
        }

        @Override
        public void run() {
            try {
                Object receivedData;
                while ((receivedData = serverIn.readObject()) != null) {
                    // TODO: Process the received move data from the server
                    // This will likely involve:
                    // 1. Deserializing receivedData to your Move object/data structure
                    // 2. Validating the move (optional, server should be authoritative)
                    // 3. Updating your local game board
                    // 4. Refreshing the GUI
                    // 5. Enabling local input for your turn
                    System.out.println("Client received move: " + receivedData);
                    // Example: ((ChessGame) yourGameInstance).processOpponentMove(receivedData);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error receiving data from server or server disconnected: " + e.getMessage());
                // Handle server disconnection
            } finally {
                disconnect();
            }
        }
    }
}