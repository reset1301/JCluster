package ru.rrr.tcp;

import com.sun.deploy.uitoolkit.ui.ConsoleHelper;
import lombok.extern.slf4j.Slf4j;
import ru.rrr.cfg.Const;
import ru.rrr.model.Message;
import ru.rrr.model.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class TcpServer {
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
                });
    }

    /**
     * Запуск серверной части
     *
     * @param port порт серверного сокета
     */
    private void startServer(int port) throws IOException {
        final ServerSocket serverSocket = createServerSocket(port);
        log.info("Node [{}]. Start server on port {}", uuid, serverSocket.getLocalPort());
        final Thread thread = new Thread(() -> {
            while (true) {
                log.info("Node [{}]. Waiting for a client on port {}...", uuid, serverSocket.getLocalPort());
                try (final Socket fromClient = serverSocket.accept()) {
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
        try (Connection connection = new Connection(fromClient)) {
            while (true) {
                try {
                    Message message = connection.receive();
                    log.info("Node [{}]. Server received message: '{}'", uuid, message);
                    Message response = handleMessage(message);
                    connection.send(message);
                    log.info("Node [{}]. Server sends message: '{}'", uuid, response);
                } catch (Exception e) {
                    log.info("Node [{}]. Current client disconnected", uuid);
                    log.info(e.toString());
                    break;
                }
            }
        } catch (IOException e) {
            log.info("Node [{}]. Current client disconnected", uuid);
            log.info(e.toString());
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
        switch (message.getType()) {
            case GET_UUID:
                log.info("Node [{}]. Calling the '{}' method on the server", uuid, MessageType.GET_UUID);
                return new Message(MessageType.GET_UUID, this.uuid);
            case GET_CLUSTER_NAME:
                log.info("Node [{}]. Calling the '{}' method on the server", uuid, MessageType.GET_CLUSTER_NAME);
                return new Message(MessageType.GET_CLUSTER_NAME, this.clusterName);
            default:
                return message;
        }
    }
}
