import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler extends Thread {
    private static final List<ClientHandler> clients = new ArrayList<>();
    private final Socket clientSocket;
    private ObjectOutputStream objectOutputStream;
    private String username;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Initialize the ObjectOutputStream for sending messages
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            Message connectMessage = Message.receiveObject(objectInputStream);
            this.username = connectMessage.getMessage();

            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage(username + " has joined the chat.");
                    System.out.println(username + " has joined the chat.");
                }
                clients.add(this);
            }

            while (true) {
                Message message = Message.receiveObject(objectInputStream);
                if (message != null) {
                    if (message.isRequest()) {
                        // Respond with the list of clients
                        objectOutputStream.writeObject(new Message(getUsers(), false, true));
                    } else if (message.isImage()) {
                        synchronized (clients) {
                            for (ClientHandler client : clients) {
                                client.sendImage(message.getByteData(),message.getUserSent());
                            }
                        }
                    } else {
                        // Handle regular messages
                        synchronized (clients) {
                            for (ClientHandler client : clients) {
                                client.sendMessage(username + ": " + message.getMessage());
                                System.out.println(username + ": " + message.getMessage());
                            }
                        }
                    }
                }

                // Adjust the sleep duration to reduce unnecessary looping
                Thread.sleep(100);
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            synchronized (clients) {
                clients.remove(this);
                for (ClientHandler client : clients) {
                    try {
                        client.sendMessage(username + " has left the chat.");
                        System.out.println(username + " has left the chat.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendImage(byte[] imageData, String userSent) throws IOException {
        if (objectOutputStream != null){
            Message.sendObjectAsync(objectOutputStream, new Message(imageData,true,false, userSent));
        }
    }

    public void sendMessage(String message) throws IOException {
        if (objectOutputStream != null) {
            // Send the Message object using ObjectOutputStream
            Message.sendObjectAsync(objectOutputStream, new Message(message, false, false));
        }
    }

    public List<String> getUsers() {
        List<String> userNames = new ArrayList<>();
        for (ClientHandler client : clients) {
            userNames.add(client.username);
        }
        return userNames;
    }
}
