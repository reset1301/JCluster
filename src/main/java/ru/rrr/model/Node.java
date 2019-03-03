package ru.rrr.model;

import lombok.extern.slf4j.Slf4j;
import ru.rrr.cfg.NodeConfig;
import ru.rrr.cfg.NodeConfigException;
import ru.rrr.client.TCPClient;
import ru.rrr.server.TCPServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Slf4j
public class Node {
    private TCPServer server;
    private Set<TCPClient> clients = new HashSet<>();

    private String address;
    private int port;

    private final String uuid = UUID.randomUUID().toString();

    public Node(NodeConfig config) throws NodeConfigException {
        if (config.getMembers().size() < 1) {
            throw new NodeConfigException("No Cluster Member URI found in configuration");
        }

        // TODO: 03.03.2019 добавить проверку по uuid и отдельный поток с переконнектами к другим нодам
        config.getMembers().forEach(uri -> clients
                .add(new TCPClient(uri.getHost(), uri.getPort(), config.getConnectionTimeoutSeconds(),
                config.getNodesDiscoverPeriod())));
    }

    private String getLocalHostName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Could not find the hostname of the local machine", e);
        }
        return hostName;
    }

    private String getLocalHostAddress() {
        String address = null;
        try {
            address = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Could not find the IP-address of the local machine", e);
        }
        return address;
    }


    public void startServer(int port) throws IOException {
        server.startServer(port);
    }

//    public void connectToServers() {
//        clients.forEach(client -> new Thread(client::reconnect).start());
//    }
}
