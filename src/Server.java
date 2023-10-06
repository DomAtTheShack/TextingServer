import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.List;

public class Server {

    private static final int PORT = 2049;

    public static void main(String[] args) {
        try {
            if (!(args.length == 0)) {
                if (args.length >= 2 || !args[0].equals("-nogui")) {
                    System.out.println("Invalid Args or Too Many");
                    System.exit(0);
                } else if (args[0].equals("-nogui")) {
                    ServerSocket serverSocket = new ServerSocket(PORT);
                    System.out.println("Server started. Waiting for clients to connect...");

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        new ClientHandler(clientSocket).start();
                    }
                }
                } else {
                    JFrame frame = new JFrame("Console Capture");
                    frame.setSize(400, 300);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                    JTextArea consoleTextArea = new JTextArea();
                    consoleTextArea.setEditable(false);

                    // Create a custom OutputStream to capture System.out.print
                    PrintStream printStream = new PrintStream(new CustomOutputStream(consoleTextArea));
                    System.setOut(printStream);

                    JScrollPane scrollPane = new JScrollPane(consoleTextArea);
                    frame.add(scrollPane);

                    frame.setVisible(true);
                    ServerSocket serverSocket = new ServerSocket(PORT);
                    System.out.println("Server started. Waiting for clients to connect...");

                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        new ClientHandler(clientSocket).start();
                    }
                }
            } catch(IOException e){
                    e.printStackTrace();
                }
            }
    private static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

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
