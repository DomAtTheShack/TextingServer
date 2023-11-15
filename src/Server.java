import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class Server {

    private static final int PORT = 2049;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Console Capture");
            frame.setSize(400, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JLabel ipLabel = new JLabel("Your Server IP: " + getLocalHost());

            JTextArea consoleTextArea = new JTextArea();
            consoleTextArea.setEditable(false);

            // Create a custom OutputStream to capture System.out.print
            PrintStream printStream = new PrintStream(new CustomOutputStream(consoleTextArea));
            System.setOut(printStream);

            JScrollPane scrollPane = new JScrollPane(consoleTextArea);

            frame.setLayout(new BorderLayout());
            frame.add(ipLabel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.setResizable(false);

            frame.setVisible(true);

            // Start the server setup in a separate thread
            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(PORT);
                    System.out.println("Server started. Waiting for clients to connect...");

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        new ClientHandler(clientSocket, 0).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private static String getLocalHost() {
        try {
            String TIP = Inet4Address.getLocalHost().toString();
            return TIP.substring(TIP.indexOf("/")+1) + ":" + PORT;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    private static class CustomOutputStream extends OutputStream {
        private final JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
