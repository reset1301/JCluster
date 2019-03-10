package ru.rrr.cluster;

import lombok.extern.slf4j.Slf4j;
import ru.rrr.cfg.Const;
import ru.rrr.cfg.NodeConfig;
import ru.rrr.cfg.NodeConfigException;
import ru.rrr.cfg.NodeUri;
import ru.rrr.model.Message;
import ru.rrr.model.MessageType;
import ru.rrr.tcp.NetworkExchangeException;
import ru.rrr.tcp.TcpClient;
import ru.rrr.tcp.TcpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.rrr.cfg.Const.PORT_56635;

@Slf4j
public class Node {
    private TcpServer server;
    private Map<String, TcpClient> clients = new HashMap<>();

    private String address;
    private int port;
    private String clusterName;

    private final String uuid = UUID.randomUUID().toString();

    private final ScheduledExecutorService discoverExecutor = Executors.newScheduledThreadPool(1);

    public Node(NodeConfig config) throws NodeConfigException, IOException {
        this.port = config.getPort();
        this.clusterName = config.getClusterName();

        log.info("Node [{}]. Start in cluster '{}'", uuid, clusterName);

        this.server = new TcpServer(port, uuid, clusterName);

        final Collection<NodeUri> members = config.getMembers();
        if (members.size() < 1) {
            throw new NodeConfigException("No Cluster Member URI found in configuration");
        }

        // TODO: 03.03.2019 добавить отдельный поток с переконнектами к другим нодам
        for (NodeUri member : members) {
            discoverExecutor.scheduleAtFixedRate(
                    () -> discover(member.getHost(), port, config.getConnectionTimeoutSeconds(),
                            config.getConnectionTimeoutSeconds()),
                    0, Const.NODE_DISCOVER_PERIOD, TimeUnit.SECONDS);
        }

    }

    /**
     * Ищет ноды своего кластера на машине host в определенном диапазоне портов.
     * Метод должен выполняться с определенной периодичностью в отдельном потоке.
     *
     * @param host
     * @param port
     * @param connectionTimeout
     * @param reconnectTimeout
     */
    private void discover(String host, int port, int connectionTimeout, int reconnectTimeout) {
        log.info("Node [{}]. Running a scan on the host: {}, starting at port {}", uuid, host, port);
        for (int currentPort = port; currentPort < PORT_56635; currentPort++) {
            TcpClient client = new TcpClient(host, currentPort, uuid, connectionTimeout, reconnectTimeout);
            if (!client.isConnected()) {
//                client.close();
                continue;
            }
            try {
                // Фильтр
                final Message messageGetUUID = client
                        .sendMessage(new Message(MessageType.GET_UUID), Const.SEND_MESSAGE_TIMEOUT);
                final String currentUUID = messageGetUUID.getData();
                if (clients.containsKey(currentUUID)) {
                    // Ранее обнаруженные ноды игнорируем
                    continue;
                }
                if (this.uuid.equals(currentUUID)) {
                    client.close();
                } else {
                    final Message messageClusterName = client
                            .sendMessage(new Message(MessageType.GET_CLUSTER_NAME), Const.SEND_MESSAGE_TIMEOUT);
                    final String currentClusterName = messageClusterName
                            .getData();
                    if (!this.clusterName.equals(currentClusterName)) {
                        client.close();
                        continue;
                    }
                }

                log.debug("New node detected: {}:{}", host, currentPort);
                // Если это другая нода из нашего кластера, то добавляем ее в коллекцию нод
                this.clients.put(currentUUID, client);
                printMembersList();
            } catch (NetworkExchangeException e) {
                client.close();
                log.error("Failed to get a reply to the message", e);
            }

        }
    }

    /**
     * Возвращает локальный hostname машины
     */
    private String getLocalHostName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Could not find the hostname of the local machine", e);
        }
        return hostName;
    }

    /**
     * Возвращает локальный IP-адрес машины
     */
    private String getLocalHostAddress() {
        String address = null;
        try {
            address = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Could not find the IP-address of the local machine", e);
        }
        return address;
    }

    /**
     * Выводит в лог текущий состав кластера: список нод
     */
    private void printMembersList() {
        StringBuilder message = new StringBuilder("members: [\n");
        List<String> membersList = new ArrayList<>();
        clients.forEach((uuid, client) -> {
            String member = String.format("uuid: %s\t[%s:%d]", uuid, client.getHost(), client.getPort());
            if (this.uuid.equals(uuid)) {
                member += "\t-\tthis";
            }
            membersList.add(member);
        });

        final String join = String.join(",\n", membersList);

        message.append(join).append("\n]");

        log.info(message.toString());
    }
}
