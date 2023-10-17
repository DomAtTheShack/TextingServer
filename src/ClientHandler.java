import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;

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
    private int[] rooms = {0,1,2};
    private int room;

    public ClientHandler(Socket socket, int room) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Initialize the ObjectOutputStream for sending messages
            objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

            Packet connectPacket = Packet.receiveObject(objectInputStream);
            this.username = connectPacket.getMessage();
            this.room = connectPacket.getRoom();

            synchronized (clients) {
                clients.add(this);
                for (ClientHandler client : clients) {
                    if(client.room == this.room) client.sendMessage(username + " has joined the chatroom "+ this.room, this.room);
                }
                System.out.println(username + " has joined the chatroom "+ this.room);
            }

            while (true) {
                Packet packet = Packet.receiveObject(objectInputStream);
                if (packet != null) {
                    if (packet.getID() == Packet.Type.UserRequest) {
                        // Respond with the list of clients
                        objectOutputStream.writeObject(new Packet(getUsers(), Packet.Type.UserRequest));
                    } else if (packet.getID() == Packet.Type.Image) {
                        synchronized (clients) {
                            for (ClientHandler client : clients) {
                                client.sendData(packet.getByteData(), packet.getUserSent(), true, packet.getRoom());
                            }
                        }
                    } else if(packet.getID() == Packet.Type.Audio){
                        synchronized (clients){
                            for(ClientHandler client : clients){
                                client.sendData(packet.getByteData(), packet.getUserSent(), false, packet.getRoom());
                            }
                        }
                    } else if (packet.getID() == Packet.Type.RoomChange){
                        synchronized (clients){
                            for(ClientHandler client: clients) {
                                if(client.username.equals(packet.getUserSent()) && validRoom(packet.getRoom())){
                                    client.changeRoom(packet.getRoom(), packet.getUserSent());
                                }
                            }
                        }
                    } else {
                        // Handle regular messages
                        synchronized (clients) {
                            for (ClientHandler client : clients) {
                                client.sendMessage(username + ": " + packet.getMessage(), packet.getRoom());
                            }
                            System.out.println(username + ": " + packet.getMessage());
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
                int rIn = this.room;
                clients.remove(this);
                for (ClientHandler client : clients) {
                    try {
                        client.sendMessage(username + " has left the chat.", rIn);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(username + " has left the chat.");
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void changeRoom(int room1, String user) {
        if (objectOutputStream != null) {
            Packet.sendObjectAsync(objectOutputStream, new Packet(room1, Packet.Type.RoomChange, user));
            room = room1;
        }
    }

    private boolean validRoom(int room) {
        for (int x: rooms){
            if(x == room) return true;
        }
        return false;
    }

    private void sendData(byte[] data, String userSent, boolean isImage, int room1) throws IOException {
        if (objectOutputStream != null){
            if(isImage) {
                Packet.sendObjectAsync(objectOutputStream, new Packet(data, Packet.Type.Image, userSent, room1));
            }else {
                Packet.sendObjectAsync(objectOutputStream, new Packet(data, Packet.Type.Audio, userSent, room1));
            }
        }
    }

    public void sendMessage(String message, int room1) throws IOException {
        if (objectOutputStream != null) {
            // Send the Message object using ObjectOutputStream
            Packet.sendObjectAsync(objectOutputStream, new Packet(message, Packet.Type.Message, room1));
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
