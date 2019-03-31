package ru.rrr.tcp;

import lombok.extern.slf4j.Slf4j;
import ru.rrr.cfg.Const;
import ru.rrr.model.Message;
import ru.rrr.model.MessageType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class TcpServer implements AutoCloseable {
    /**
     * Порт, на котором поднимается серверный сокет
     */
    private int port;
    /**
     * Пул потоков для обработки клиентских соединений
     */
    private final ExecutorService exec;

    private final String uuid;
    private final String clusterName;

    private final AtomicBoolean isRunning = new AtomicBoolean();

    /**
     * Конструктор
     *
     * @param port        порт
     * @param uuid        идентификатор ноды в кластере
     * @param clusterName название кластера
     */
    public TcpServer(int port, String uuid, String clusterName) throws IOException {
        this.port = port;
        this.uuid = uuid;
        this.clusterName = clusterName;

        startServer(port);

        // TODO: 03.03.2019 возможно, не лучшее решение. Нужно посмотреть исходники.
        // Тредпул с daemon-потоками
        exec = Executors.newCachedThreadPool(
                r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
        );
    }

    public int getPort() {
        return port;
    }

    /**
     * Запуск серверной части
     *
     * @param port порт серверного сокета
     */
    private void startServer(int port) throws IOException {
        final ServerSocket serverSocket = createServerSocket(port);
        this.isRunning.set(true);
        log.info("Node [{}]. Start server on port {} in cluster '{}'", uuid, serverSocket.getLocalPort(), clusterName);
        final Thread thread = new Thread(() -> {
            while (isRunning.get()) {
                log.info("Node [{}]. Waiting for a client on port {}...", uuid, serverSocket.getLocalPort());
                try {
                    final Socket fromClient = serverSocket.accept();
                    log.info("Node [{}]. Current client connected {}:{}", uuid,
                            fromClient.getInetAddress().getHostAddress(), fromClient.getPort());
                    exec.submit(() -> runClient(fromClient));
                } catch (IOException e) {
                    log.warn("Node [{}]. Can't accept", uuid);
                }
            }
        });

        thread.setDaemon(true);

        thread.start();
    }

    /**
     * @param port порт, с которого начинаются попытки поднять сервер
     * @return ServerSocket серверный сокет
     * @throws IOException в случае, если не удалось открыть серверный сокет на машине
     */
    private ServerSocket createServerSocket(int port) throws IOException {
        for (int currentPort = port; currentPort < Const.PORT_56635; currentPort++) {
            try {
                final ServerSocket serverSocket = new ServerSocket(currentPort);
                this.port = currentPort;
                return serverSocket;
            } catch (IOException e) {
                log.info("Node [{}]. Port {} is already busy", uuid, currentPort);
            }
        }

        throw new IOException("Node {" + uuid + "}. Could not start server");

    }

    /**
     * Обмен данными с клиентом по TCP
     *
     * @param fromClient сокет от клиента
     */
    private void runClient(Socket fromClient) {
        final InetAddress host = fromClient.getInetAddress();
        final int port = fromClient.getPort();
        try (Connection connection = new Connection(fromClient)) {
            while (isRunning.get()) {
                try {
                    Message message = connection.receive();
                    log.info("Node [{}]. Server received message: '{}'", uuid, message);
                    Message response = handleMessage(message);
                    connection.send(response);
                    log.info("Node [{}]. Server sends message: '{}'", uuid, response);
                    if (Objects.equals(message.getType(), MessageType.CLOSE_CONNECTION)) {
                        log.info("Received a message about closing the connection from the client {}:{}", host, port);
                        break;
                    }
                } catch (Exception e) {
                    log.error("Node [" + uuid + "]. Error while processing a message from a client, '" + host + ":"
                            + port + "'", e);
                    break;
                }
            }
            log.info("Node [{}]. Client {}:{} disconnected", uuid, host, port);
        } catch (IOException e) {
            log.error("Node [" + uuid + "]. Client " + host + ":" + port + " disconnected", e);
        }
    }

    // TODO: 10.03.2019 Идентифицировать клиента

    /**
     * Обработка входящих сообщений
     *
     * @param message сообщение от клиента
     * @return String ответ
     */
    public Message handleMessage(Message message) {
        log.info("Node [{}]. Calling the '{}' method on the server", uuid, message.getType());
        switch (message.getType()) {
            case GET_UUID:
                return new Message(MessageType.GET_UUID, this.uuid);
            case GET_CLUSTER_NAME:
                return new Message(MessageType.GET_CLUSTER_NAME, this.clusterName);
            default:
                return message;
        }
    }

    @Override
    public void close() {
        this.isRunning.set(false);
    }
}
