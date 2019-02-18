package ru.rrr.model;

import org.springframework.stereotype.Component;
import ru.rrr.client.ClientSocketFactory;
import ru.rrr.server.TCPServer;

@Component
public class Node {
    private TCPServer server = new TCPServer();
    private ClientSocketFactory factory = new ClientSocketFactory();

    public void startServer(int port) {
        server.startServer(port);
    }

    public void connectToServer(String ip, int port) {
        factory.reconnect(ip, port);
    }
}
