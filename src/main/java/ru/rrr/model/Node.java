package ru.rrr.model;

import lombok.Data;
import org.springframework.stereotype.Component;
import ru.rrr.client.TCPClient;
import ru.rrr.server.TCPServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
@Data
public class Node {
    private TCPServer server = new TCPServer();
    private List<TCPClient> clients = new ArrayList<>();

    public void startServer(int port) {
        server.startServer(port);
    }

    public void connectToServers() {
        clients.forEach(client -> new Thread(client::reconnect).start());
    }
}
