package ru.rrr.model;

import lombok.Data;
import org.springframework.stereotype.Component;
import ru.rrr.client.TCPClient;
import ru.rrr.server.TCPServer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
@Data
public class Node implements Serializable {
    private int countClients = 0;
    private TCPServer server = new TCPServer();
    private List<TCPClient> clients = new ArrayList<>();

    public void startServer(int port) {
        server.startServer(port);
    }

    public void connectToServers() {
        clients.forEach(client -> new Thread(client::reconnect).start());
    }
}
