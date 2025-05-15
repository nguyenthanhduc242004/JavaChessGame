package org.example.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ChessGameLAN extends JFrame {

    private JTextArea gameLog;
    private ChessServer chessServer;
    private ChessClient chessClient;

    public ChessGameLAN() {
        setTitle("LAN Chess Game");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menu Bar
        JMenuBar menuBar = getBar();
        setJMenuBar(menuBar);

        // Simple Game Log Area
        gameLog = new JTextArea();
        gameLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(gameLog);
        add(scrollPane, BorderLayout.CENTER);

        // Placeholder for the actual chess board GUI
        JPanel chessBoardPanel = new JPanel();
        chessBoardPanel.setPreferredSize(new Dimension(400, 400)); // Example size
        chessBoardPanel.setBackground(Color.LIGHT_GRAY);
        JLabel boardLabel = new JLabel("Chess Board Area");
        chessBoardPanel.add(boardLabel);
        // add(chessBoardPanel, BorderLayout.WEST); // Or your preferred layout

        logMessage("Welcome to LAN Chess. Use the Game menu to start.");
    }

    private JMenuBar getBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem hostMenuItem = new JMenuItem("Host Game");
        JMenuItem joinMenuItem = new JMenuItem("Join Game");
        JMenuItem exitMenuItem = new JMenuItem("Exit");

        hostMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hostGame();
            }
        });

        joinMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinGame();
            }
        });

        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        gameMenu.add(hostMenuItem);
        gameMenu.add(joinMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(exitMenuItem);
        menuBar.add(gameMenu);
        return menuBar;
    }

    private void logMessage(String message) {
        gameLog.append(message + "\n");
    }

    private void hostGame() {
        if (chessServer != null && chessServer.isRunning()) {
            JOptionPane.showMessageDialog(this, "Server is already running.", "Server Active", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (chessClient != null && chessClient.isConnected()) {
            JOptionPane.showMessageDialog(this, "Cannot host while connected as a client.", "Client Active", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String portStr = JOptionPane.showInputDialog(this, "Enter port number to host on (e.g., 5000):", "Host Game", JOptionPane.PLAIN_MESSAGE, null, null, "5000").toString();
        if (portStr == null || portStr.trim().isEmpty()) {
            logMessage("Host game cancelled.");
            return;
        }

        try {
            int port = Integer.parseInt(portStr.trim());
            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Invalid port number. Please use a port between 1024 and 65535.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Display IP addresses
            List<String> localIPs = getLocalIPAddresses();
            String ipMessage = "Attempting to host on port " + port + ".\n" +
                    "Other player should use one of these IP addresses to connect:\n";
            if (localIPs.isEmpty()) {
                ipMessage += "Could not automatically determine IP addresses. Please find your LAN IP manually.\n";
            } else {
                for (String ip : localIPs) {
                    ipMessage += ip + "\n";
                }
            }
            logMessage(ipMessage);
            JOptionPane.showMessageDialog(this, ipMessage, "Server IP Information", JOptionPane.INFORMATION_MESSAGE);


            chessServer = new ChessServer(port, this::logMessage); // Pass logger
            Thread serverThread = new Thread(chessServer::startServer); // Use a new thread for server
            serverThread.setName("ChessServerThread");
            serverThread.start();

            // You might want to disable "Host Game" and "Join Game" menu items here
            // and enable them again if the server stops or connection fails.

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number. Please enter a numeric value.", "Error", JOptionPane.ERROR_MESSAGE);
            logMessage("Error hosting game: Invalid port format.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not start server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            logMessage("Error hosting game: " + ex.getMessage());
        }
    }

    private void joinGame() {
        if (chessClient != null && chessClient.isConnected()) {
            JOptionPane.showMessageDialog(this, "Already connected to a game.", "Client Active", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (chessServer != null && chessServer.isRunning()) {
            JOptionPane.showMessageDialog(this, "Cannot join a game while hosting.", "Server Active", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField ipField = new JTextField("localhost");
        JTextField portField = new JTextField("5000");
        Object[] message = {
                "Server IP Address:", ipField,
                "Server Port:", portField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Join Game", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String ipAddress = ipField.getText().trim();
            String portStr = portField.getText().trim();

            if (ipAddress.isEmpty() || portStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "IP Address and Port cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int port = Integer.parseInt(portStr);
                if (port < 1024 || port > 65535) {
                    JOptionPane.showMessageDialog(this, "Invalid port number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                logMessage("Attempting to connect to " + ipAddress + ":" + port + "...");
                chessClient = new ChessClient(ipAddress, port, this::logMessage); // Pass logger
                Thread clientThread = new Thread(chessClient::connect); // Use a new thread for client connection
                clientThread.setName("ChessClientThread");
                clientThread.start();

                // You might want to disable "Host Game" and "Join Game" menu items here

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number. Please enter a numeric value.", "Error", JOptionPane.ERROR_MESSAGE);
                logMessage("Error joining game: Invalid port format.");
            }
        } else {
            logMessage("Join game cancelled.");
        }
    }

    public static List<String> getLocalIPAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) { // Skip loopback, down, virtual interfaces
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ia = inetAddresses.nextElement();
                    if (ia instanceof Inet4Address && !ia.isLoopbackAddress()) { // Prefer IPv4, not loopback
                        ipAddresses.add(ia.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Could not retrieve IP addresses: " + e.getMessage());
            // Optionally log to GUI as well
        }
        return ipAddresses;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChessGameLAN().setVisible(true);
            }
        });
    }
}

// ---- Placeholder Server and Client Classes ----
// These would contain the actual socket programming logic

interface LoggerCallback {
    void log(String message);
}

class ChessServer {
    private int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private LoggerCallback logger;


    public ChessServer(int port, LoggerCallback logger) {
        this.port = port;
        this.logger = logger;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.log("Server started on port: " + port + ". Waiting for a client...");

            // This is a blocking call, a real server would handle this in a loop for multiple games or manage connections
            Socket clientSocket = serverSocket.accept(); // Waits for one client
            running = true; // Set to true once connection is accepted for this simple example
            logger.log("Client connected: " + clientSocket.getInetAddress().getHostAddress());

            // TODO: Implement actual communication logic here
            // e.g., create input/output streams, game loop
            // For now, just keep the connection "alive" conceptually
            // In a real game, you'd have a loop here reading/writing data
            // and it might end when the game is over or connection lost.

            // Simulating some activity or keeping connection
            try {
                // Keep the thread alive until explicitly stopped or connection drops
                // In a real server, this thread would handle communication with the client
                while(running && !clientSocket.isClosed()){
                    // Check for incoming messages or send keep-alives, etc.
                    // For this placeholder, we'll just sleep.
                    // If client disconnects, an IOException will likely occur during read/write.
                    Thread.sleep(1000); // Placeholder
                    if (clientSocket.getInputStream().available() > 0) {
                        // Dummy read to detect disconnection sooner in some cases
                        // In a real app, you'd be actively reading game data
                        clientSocket.getInputStream().read();
                    }
                }
            } catch (SocketException se) {
                logger.log("Client disconnected: " + se.getMessage());
            }
            catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // Restore interrupt status
                logger.log("Server communication thread interrupted.");
            } catch (IOException e) {
                logger.log("IO Error during client communication: " + e.getMessage());
            } finally {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    try {clientSocket.close();} catch (IOException io) {logger.log("Error closing client socket: " + io.getMessage());}
                }
                stopServer(); // Ensure server resources are cleaned up
            }

        } catch (IOException e) {
            if (running) { // Avoid error message if stopServer() was called
                logger.log("Server Error: Could not listen on port " + port + ". " + e.getMessage());
            }
            running = false;
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.log("Error closing server socket: " + e.getMessage());
                }
            }
            logger.log("Server has stopped.");
            running = false;
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // This will interrupt the accept() call if it's blocking
                logger.log("Server socket closed.");
            }
        } catch (IOException e) {
            logger.log("Error while stopping server: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }
}

class ChessClient {
    private String serverIp;
    private int serverPort;
    private Socket socket;
    private boolean connected = false;
    private LoggerCallback logger;

    public ChessClient(String serverIp, int serverPort, LoggerCallback logger) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.logger = logger;
    }

    public void connect() {
        try {
            socket = new Socket(); // Create an unbound socket
            // Connect with a timeout to prevent GUI freeze if server is unreachable
            socket.connect(new InetSocketAddress(serverIp, serverPort), 5000); // 5 seconds timeout
            connected = true;
            logger.log("Successfully connected to server: " + serverIp + ":" + serverPort);

            // TODO: Implement actual communication logic here
            // e.g., create input/output streams, game loop
            // For now, just keep the connection "alive"
            // In a real game, you'd have a loop here reading/writing data
            while(connected && !socket.isClosed()){
                // Check for incoming messages or send keep-alives, etc.
                // For this placeholder, we'll just sleep.
                // If server disconnects, an IOException will likely occur during read/write.
                Thread.sleep(1000); // Placeholder
                if (socket.getInputStream().available() > 0) {
                    // Dummy read to detect disconnection sooner in some cases
                    socket.getInputStream().read();
                }
            }

        } catch (SocketTimeoutException ste) {
            logger.log("Connection timed out. Server not responding at " + serverIp + ":" + serverPort);
            connected = false;
        }
        catch (IOException e) {
            logger.log("Client Error: Could not connect to server " + serverIp + ":" + serverPort + ". " + e.getMessage());
            connected = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log("Client communication thread interrupted.");
            connected = false;
        }
        finally {
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.log("Error closing client socket: " + e.getMessage());
                }
            }
            logger.log("Client connection attempt finished.");
            connected = false; // Ensure connected is false if loop exits
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.log("Disconnected from server.");
            }
        } catch (IOException e) {
            logger.log("Error while disconnecting: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
