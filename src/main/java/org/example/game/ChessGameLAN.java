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

    private JMenuItem hostMenuItem;
    private JMenuItem joinMenuItem;
    private JMenuItem stopHostMenuItem;
    private JMenuItem exitMenuItem;


    public ChessGameLAN() {
        setTitle("LAN Chess Game - " + getCurrentDate()); // Added date to title
        setSize(700, 500); // Increased size slightly
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        hostMenuItem = new JMenuItem("Host Game");
        joinMenuItem = new JMenuItem("Join Game");
        stopHostMenuItem = new JMenuItem("Stop Hosting");
        exitMenuItem = new JMenuItem("Exit");

        hostMenuItem.addActionListener(e -> hostGame());
        joinMenuItem.addActionListener(e -> joinGame());
        stopHostMenuItem.addActionListener(e -> stopHostingRequested());
        exitMenuItem.addActionListener(e -> System.exit(0));

        gameMenu.add(hostMenuItem);
        gameMenu.add(joinMenuItem);
        gameMenu.add(stopHostMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(exitMenuItem);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);

        // Simple Game Log Area
        gameLog = new JTextArea();
        gameLog.setEditable(false);
        gameLog.setLineWrap(true); // Enable line wrapping
        gameLog.setWrapStyleWord(true); // Wrap at word boundaries
        JScrollPane scrollPane = new JScrollPane(gameLog);
        add(scrollPane, BorderLayout.CENTER);

        // Placeholder for the actual chess board GUI
        JPanel chessBoardPanel = new JPanel();
        chessBoardPanel.setPreferredSize(new Dimension(450, 450)); // Example size
        chessBoardPanel.setBackground(Color.LIGHT_GRAY);
        JLabel boardLabel = new JLabel("Chess Board Area (GUI Placeholder)");
        boardLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chessBoardPanel.add(boardLabel);
        // add(chessBoardPanel, BorderLayout.WEST); // Or your preferred layout for the board

        logMessage("Welcome to LAN Chess. Use the Game menu to start or join a game.");
        updateMenuStates(); // Initial menu state
    }

    private String getCurrentDate() {
        // In a real application, you might use SimpleDateFormat, but for simplicity:
        return java.time.LocalDate.now().toString();
    }


    private void logMessage(String message) {
        // Ensure GUI updates are on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            gameLog.append(message + "\n");
            gameLog.setCaretPosition(gameLog.getDocument().getLength()); // Auto-scroll
        });
    }

    private void updateMenuStates() {
        // Ensure this is always called on the EDT
        SwingUtilities.invokeLater(() -> {
            boolean serverActuallyRunning = (chessServer != null && chessServer.isRunning());
            boolean clientActuallyConnected = (chessClient != null && chessClient.isConnected());

            hostMenuItem.setEnabled(!serverActuallyRunning && !clientActuallyConnected);
            joinMenuItem.setEnabled(!serverActuallyRunning && !clientActuallyConnected);

            // Visibility and enabled state for stopHostMenuItem
            stopHostMenuItem.setEnabled(serverActuallyRunning);
            stopHostMenuItem.setVisible(serverActuallyRunning);
        });
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
            logMessage("Host game cancelled by user.");
            return;
        }

        try {
            int port = Integer.parseInt(portStr.trim());
            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Invalid port number. Please use a port between 1024 and 65535.", "Port Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<String> localIPs = getLocalIPAddresses();
            StringBuilder ipMessageBuilder = new StringBuilder("Attempting to host on port " + port + ".\n");
            if (localIPs.isEmpty()) {
                ipMessageBuilder.append("Could not automatically determine local IP addresses. Please find your LAN IP manually (e.g., using ipconfig/ifconfig).\n");
            } else {
                ipMessageBuilder.append("Other player should try connecting to one of these IP addresses on your LAN:\n");
                for (String ip : localIPs) {
                    ipMessageBuilder.append("- ").append(ip).append("\n");
                }
            }
            ipMessageBuilder.append("\nIf connection fails, ensure your firewall allows connections on port ").append(port).append(".");

            logMessage(ipMessageBuilder.toString());
            JOptionPane.showMessageDialog(this, ipMessageBuilder.toString(), "Server IP Information", JOptionPane.INFORMATION_MESSAGE);

            chessServer = new ChessServer(port, this::logMessage, this::onServerStopped); // Pass logger and stop callback
            Thread serverThread = new Thread(chessServer::startServer);
            serverThread.setName("ChessServerThread");
            serverThread.setDaemon(true); // Allow application to exit if only daemon threads are running
            serverThread.start();

            updateMenuStates(); // Server is attempting to start

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid port number. Please enter a numeric value.", "Input Error", JOptionPane.ERROR_MESSAGE);
            logMessage("Error hosting game: Invalid port format '" + portStr + "'.");
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

        JTextField ipField = new JTextField("localhost"); // Default to localhost
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

                logMessage("Attempting to connect to server at " + ipAddress + ":" + port + "...");
                chessClient = new ChessClient(ipAddress, port, this::logMessage, this::onClientDisconnected); // Pass logger & disconnect callback
                Thread clientThread = new Thread(chessClient::connect);
                clientThread.setName("ChessClientThread");
                clientThread.setDaemon(true);
                clientThread.start();

                updateMenuStates(); // Client is attempting to connect

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number. Please enter a numeric value.", "Input Error", JOptionPane.ERROR_MESSAGE);
                logMessage("Error joining game: Invalid port format '" + portStr + "'.");
            }
        } else {
            logMessage("Join game cancelled by user.");
        }
    }

    private void stopHostingRequested() {
        if (chessServer != null && chessServer.isRunning()) {
            logMessage("User requested to stop hosting...");
            chessServer.stopServer();
            // onServerStopped callback will handle UI updates
        } else {
            logMessage("Stop hosting requested, but server was not running.");
            updateMenuStates(); // Ensure UI is consistent
        }
    }

    // Callback from ChessServer when it has fully stopped
    private void onServerStopped() {
        logMessage("Server has confirmed it is stopped.");
        chessServer = null; // Clear the server instance
        SwingUtilities.invokeLater(this::updateMenuStates);
    }

    // Callback from ChessClient when it has disconnected
    private void onClientDisconnected() {
        logMessage("Client has confirmed disconnection or connection failure.");
        chessClient = null; // Clear the client instance
        SwingUtilities.invokeLater(this::updateMenuStates);
    }


    public static List<String> getLocalIPAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual() || ni.getName().startsWith("vmnet") || ni.getName().startsWith("vboxnet")) {
                    continue;
                }
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress ia = inetAddresses.nextElement();
                    if (ia instanceof Inet4Address && !ia.isLoopbackAddress()) {
                        ipAddresses.add(ia.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Could not retrieve IP addresses: " + e.getMessage());
        }
        return ipAddresses;
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessGameLAN().setVisible(true));
    }
}

// ---- Logger and Callback Interfaces ----
@FunctionalInterface
interface LoggerCallback {
    void log(String message);
}

@FunctionalInterface
interface ServerStopCallback {
    void onServerStopped();
}

@FunctionalInterface
interface ServerStartCallback {
    void onServerStartedSuccessfully();
}

@FunctionalInterface
interface ClientDisconnectCallback {
    void onClientDisconnected();
}


// ---- Modified Server and Client Classes ----

class ChessServer {
    private final int port;
    private ServerSocket serverSocket;
    private Socket clientSocket; // Single client
    private volatile boolean running = false; // Ensure visibility across threads
    private final LoggerCallback logger;
    private final ServerStopCallback stopCallback;
    private Thread communicationThread;


    public ChessServer(int port, LoggerCallback logger, ServerStopCallback stopCallback) {
        this.port = port;
        this.logger = logger;
        this.stopCallback = stopCallback;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            // Successfully bound the port, now set running to true
            // This needs to be set before accept() so isRunning() is accurate
            // But if accept fails, it needs to be reset.
            // Let's set running true *after* successful accept,
            // or handle BindException specifically before that.
            logger.log("Server socket created. Listening on port: " + port + " for a single client...");
            running = true; // Server is now officially attempting to run
            // Crucially, update GUI state *before* blocking on accept()
            // This is handled by the caller (ChessGameLAN) updating menus after starting the thread.

            clientSocket = serverSocket.accept(); // Blocks until one client connects
            // Only one client is accepted. Close server socket to prevent more.
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close(); // No more new connections
                    logger.log("Server socket closed. No longer accepting new connections.");
                } catch (IOException e) {
                    logger.log("Warning: Error closing server socket after client connected: " + e.getMessage());
                }
            }

            logger.log("Client connected: " + clientSocket.getInetAddress().getHostAddress() + ". Ready for game.");
            // Server is running and has a client.

            // Communication loop with the single client
            communicationThread = Thread.currentThread(); // Store for potential interrupt
            while (running && clientSocket != null && !clientSocket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    // TODO: Implement actual game data exchange here
                    // Example: Read move object, process, send response
                    // For this placeholder, we'll check if client is still there.
                    if (clientSocket.getInputStream().read() == -1) {
                        logger.log("Client disconnected (EOF reached).");
                        break; // Client closed connection
                    }
                    // Simulate some processing
                    Thread.sleep(200); // Check periodically
                } catch (SocketException se) {
                    if (running) logger.log("Client connection issue: " + se.getMessage() + ". Assuming client disconnected.");
                    break;
                } catch (IOException e) {
                    if (running) logger.log("IO Error during client communication: " + e.getMessage());
                    break; // Error in communication
                } catch (InterruptedException e) {
                    logger.log("Server communication thread interrupted.");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    break;
                }
            }

        } catch (BindException e) {
            logger.log("SERVER ERROR: Port " + port + " is already in use. Cannot start server. " + e.getMessage());
            // running should remain false or be set to false
            running = false; // Ensure server is marked as not running
        } catch (SocketException e) {
            if (running) { // If stopServer() was called, serverSocket.close() will cause this.
                logger.log("Server socket closed or error: " + e.getMessage() + ". Server stopping.");
            }
        } catch (IOException e) {
            if (running) {
                logger.log("Server IO Error: " + e.getMessage());
            }
        } finally {
            cleanUpResources();
            if (stopCallback != null) {
                stopCallback.onServerStopped(); // Notify GUI that server has fully stopped
            }
        }
    }

    private void cleanUpResources() {
        running = false; // Ensure it's marked as not running
        logger.log("Server cleaning up resources...");
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
                logger.log("Client socket closed.");
            }
        } catch (IOException e) {
            logger.log("Error closing client socket: " + e.getMessage());
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                logger.log("Main server socket closed.");
            }
        } catch (IOException e) {
            logger.log("Error closing main server socket: " + e.getMessage());
        }
        logger.log("Server cleanup finished.");
    }

    public void stopServer() {
        logger.log("Stop server initiated...");
        running = false; // Signal the loop to stop

        // Interrupt the communication thread if it's stuck in a blocking operation (like read or sleep)
        if (communicationThread != null && communicationThread.isAlive()) {
            communicationThread.interrupt();
        }

        // Closing sockets will also help unblock threads and cause exceptions that lead to loop termination
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // This will break out of accept() if it was waiting
            }
        } catch (IOException e) {
            logger.log("Error closing server socket during stop: " + e.getMessage());
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            logger.log("Error closing client socket during stop: " + e.getMessage());
        }
        // The finally block in startServer() will call the stopCallback
    }

    public boolean isRunning() {
        // More robust check: is the server socket bound and not closed (before client connects)
        // or is clientSocket connected (after client connects)?
        // For simplicity, relying on the volatile `running` flag, which is managed by start/stop logic.
        return running;
    }
}

class ChessClient {
    private final String serverIp;
    private final int serverPort;
    private Socket socket;
    private volatile boolean connected = false; // Ensure visibility
    private final LoggerCallback logger;
    private final ClientDisconnectCallback disconnectCallback;
    private Thread communicationThread;

    public ChessClient(String serverIp, int serverPort, LoggerCallback logger, ClientDisconnectCallback dcCallback) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.logger = logger;
        this.disconnectCallback = dcCallback;
    }

    public void connect() {
        communicationThread = Thread.currentThread();
        try {
            socket = new Socket();
            logger.log("Client attempting to connect to " + serverIp + ":" + serverPort + " (timeout 5s)...");
            socket.connect(new InetSocketAddress(serverIp, serverPort), 5000); // 5 seconds timeout
            connected = true;
            logger.log("CLIENT: Successfully connected to server: " + serverIp + ":" + serverPort);

            // TODO: Implement actual game data exchange here
            while (connected && socket != null && !socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Example: Read move object, process, send response
                    if (socket.getInputStream().read() == -1) { // Check for server disconnect
                        logger.log("CLIENT: Server disconnected (EOF reached).");
                        break;
                    }
                    Thread.sleep(200); // Simulate activity or keep-alive check
                } catch (SocketException se) {
                    if(connected) logger.log("CLIENT: Connection issue: " + se.getMessage() + ". Assuming server disconnected.");
                    break;
                } catch (IOException e) {
                    if(connected) logger.log("CLIENT: IO Error: " + e.getMessage());
                    break;
                } catch (InterruptedException e) {
                    logger.log("CLIENT: Communication thread interrupted.");
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    break;
                }
            }
        } catch (SocketTimeoutException ste) {
            logger.log("CLIENT: Connection timed out. Server not responding at " + serverIp + ":" + serverPort);
        } catch (ConnectException ce) {
            logger.log("CLIENT: Connection refused at " + serverIp + ":" + serverPort + ". Is the server running and port correct? " + ce.getMessage());
        } catch (UnknownHostException uhe) {
            logger.log("CLIENT: Unknown host: " + serverIp + ". Check the IP address. " + uhe.getMessage());
        }
        catch (IOException e) {
            logger.log("CLIENT: Could not connect to server " + serverIp + ":" + serverPort + ". " + e.getMessage());
        } finally {
            cleanUpConnection();
            if (disconnectCallback != null) {
                disconnectCallback.onClientDisconnected();
            }
        }
    }

    private void cleanUpConnection() {
        connected = false; // Ensure status is updated
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.log("CLIENT: Socket closed.");
            }
        } catch (IOException e) {
            logger.log("CLIENT: Error closing client socket: " + e.getMessage());
        }
        logger.log("CLIENT: Connection attempt/session finished.");
    }


    public void disconnect() {
        logger.log("CLIENT: Disconnect requested by user/app.");
        connected = false; // Signal the loop to stop
        if (communicationThread != null && communicationThread.isAlive()) {
            communicationThread.interrupt(); // Interrupt if stuck
        }
        // The finally block in connect() will handle actual socket closing and callback.
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}