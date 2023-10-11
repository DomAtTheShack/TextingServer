import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends Thread {
    private static final List<ClientHandler> clients = new ArrayList<>();
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
            System.out.println(username + " has connected.");
            out.println(username + " has connected.");

            synchronized (clients) {
                for (ClientHandler client : clients) {
                    System.out.println(username + " has joined the chat.");
                    client.sendMessage(username + " has joined the chat.");
                }
                clients.add(this);
            }

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equals("GET_CLIENTS")) {
                    // Respond with the list of clients
                        out.println(listToArray(clients));
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
                    System.out.println(username + " has left the chat.");
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

    private String listToArray(List<ClientHandler> clients) {
        StringBuilder x = new StringBuilder();
        x.append("CLIENT_LIST:");
        for(int i = 0;i<clients.size();i++){
            x.append(clients.get(i).username+",");
        }
        return x.toString();
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
