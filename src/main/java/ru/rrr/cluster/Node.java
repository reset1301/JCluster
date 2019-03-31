package ru.rrr.cluster;

import lombok.extern.slf4j.Slf4j;
import ru.rrr.cfg.Const;
import ru.rrr.cfg.NodeConfig;
import ru.rrr.cfg.NodeConfigException;
import ru.rrr.cfg.NodeUri;
import ru.rrr.cluster.event.ClusterEvent;
import ru.rrr.cluster.event.ClusterEventListener;
import ru.rrr.cluster.event.MemberDescription;
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

    private List<ClusterEventListener> clusterEventListeners = new ArrayList<>();

    private String address;
    private int port;
    private String clusterName;

    private final String uuid = UUID.randomUUID().toString();

    private final ScheduledExecutorService discoverExecutor = Executors.newScheduledThreadPool(1);
    private final int sendMessageTimeout;

    public Node(NodeConfig config) throws NodeConfigException, IOException {
        this.port = config.getPort();
        this.clusterName = config.getClusterName();

        initListeners();

        // TODO: 31.03.2019 этот параметр должен инициализироваться из конфига, или константой, если в конфиге не задан
        this.sendMessageTimeout = Const.SEND_MESSAGE_TIMEOUT;

        log.info("Node [{}]. Start in cluster '{}'", uuid, clusterName);

        this.server = new TcpServer(port, uuid, clusterName);
        this.port = server.getPort();

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
     * Настройка слушателей событий кластера
     */
    private void initListeners() {
        this.clusterEventListeners.add(new ClusterEventListener() {
            @Override
            public void onClusterEvent(ClusterEvent event) {
                printMembersList();
            }

            @Override
            public void onMemberAdd(MemberDescription memberDescription) {
                printMembersList();
            }

            @Override
            public void onMemberRemove(MemberDescription memberDescription) {
                printMembersList();
            }
        });
    }

    public void addClusterEventListener(ClusterEventListener listener) {
        this.clusterEventListeners.add(listener);
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
                // TODO: 31.03.2019 это надо как-то обрабатывать
                continue;
            }
            try {
                // Фильтр
                final Message messageGetUUID = client
                        .sendMessage(new Message(MessageType.GET_UUID), sendMessageTimeout);
                final String currentUUID = messageGetUUID.getData();
                if (clients.containsKey(currentUUID)) {
                    // Ранее обнаруженные ноды игнорируем
                    continue;
                }
                if (this.uuid.equals(currentUUID)) {
                    this.address = client.getHost();
                    printMembersList();
                    closeSelfConnection(client);
                    continue;
                } else {
                    final Message messageClusterName = client
                            .sendMessage(new Message(MessageType.GET_CLUSTER_NAME), Const.SEND_MESSAGE_TIMEOUT);
                    final String currentClusterName = messageClusterName.getData();
                    if (!this.clusterName.equals(currentClusterName)) {
                        closeOtherClusterConnection(client, currentClusterName);
                        continue;
                    }
                }

                log.debug("New node detected: {}:{}", host, currentPort);
                // Если это другая нода из нашего кластера, то добавляем ее в коллекцию нод
                addNewClient(currentUUID, client);
            } catch (NetworkExchangeException e) {
                client.close();
                log.error("Failed to get a reply to the message", e);
            }

        }
    }

    /**
     * Добавляет новую ноду в кластер
     *
     * @param uuid   идентификатор ноды
     * @param client клиент
     */
    private void addNewClient(String uuid, TcpClient client) {
        for (ClusterEventListener listener : clusterEventListeners) {
            listener.onMemberAdd(new MemberDescription(uuid, client.getHost(), client.getPort()));
        }
        this.clients.put(uuid, client);
        printMembersList();
    }

    /**
     * Закрывает соединение ноды к самой себе
     *
     * @param client клиент
     * @throws NetworkExchangeException
     */
    private void closeSelfConnection(TcpClient client) throws NetworkExchangeException {
        // Закрываем соединение с самим собой
        log.info("Node [{}] found itself. This connection will be closed.", uuid);
        client.sendMessage(new Message(MessageType.CLOSE_CONNECTION), Const.SEND_MESSAGE_TIMEOUT);
        if (client.isConnected()) {
            client.close();
        }
    }

    /**
     * Закрывает соединение ноды к серверу из чужого кластера
     *
     * @param client         клиент
     * @param anotherCluster
     * @throws NetworkExchangeException
     */
    private void closeOtherClusterConnection(TcpClient client, String anotherCluster) throws NetworkExchangeException {
        log.info("Discovered a node from another cluster '{}'. The connection will be closed.", anotherCluster);
        client.sendMessage(new Message(MessageType.CLOSE_CONNECTION), Const.SEND_MESSAGE_TIMEOUT);
        if (client.isConnected()) {
            client.close();
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
        StringBuilder message = new StringBuilder("Node [" + uuid + "]. members: [\n");
        List<String> membersList = new ArrayList<>();
        membersList.add(String.format("uuid: %s\t[%s:%d]\t-\tthis", uuid, this.address, this.port));
        clients.forEach((uuid, client) -> {
            membersList.add(String.format("uuid: %s\t[%s:%d]", uuid, client.getHost(), client.getPort()));
        });

        final String join = String.join(",\n", membersList);

        message.append(join).append("\n]");

        log.info(message.toString());
    }
}
