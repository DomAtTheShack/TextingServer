import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends Thread {
    private static List<ClientHandler> clients = new ArrayList<>();
    private Socket clientSocket;
    private PrintWriter out;
    private String username;


    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            this.username = in.readLine();
            out.println(username + " has connected.");

            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage(username + " has joined the chat.");
                }
                clients.add(this);
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("GET_CLIENTS")) {
                    // Respond with the list of clients
                    synchronized (clients) {
                        StringBuilder clientList = new StringBuilder("CLIENT_LIST:");
                        for (ClientHandler client : clients) {
                            clientList.append(client.getUsername()).append(",");
                        }
                        out.println(clientList.toString());
                    }
                } else {
                    // Handle regular messages
                    synchronized (clients) {
                        for (ClientHandler client : clients) {
                            client.sendMessage(username + ": " + message);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (clients) {
                clients.remove(this);
                for (ClientHandler client : clients) {
                    client.sendMessage(username + " has left the chat.");
                }
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    public String getUsername() {
        return username;
    }
}
