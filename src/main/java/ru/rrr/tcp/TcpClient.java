package ru.rrr.tcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.rrr.model.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO: 03.03.2019 Вынести общий функционал в интерфейс/абстрактный класс типа ClusterClient с разными имплементациями
@Slf4j
@Data
public class TcpClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final String uuid;
    private Socket socket;
    private Connection connection;
    private final int reconnectTimeout;
    private final int connectionTimeout;
    private final ExecutorService messageExecutor = Executors.newCachedThreadPool();

    public TcpClient(String host, int port, String uuid, int connectionTimeout, int reconnectTimeout) {
        this.host = host;
        this.port = port;
        this.uuid = uuid;
        this.connectionTimeout = connectionTimeout;
        this.reconnectTimeout = reconnectTimeout;
        try {
            this.connection = connect();
        } catch (IOException e) {
            log.debug("Node [{}]. Could not connect to server {}:{}", uuid, host, port, e);
        }

    }

    /**
     * @return boolean true - connected, false - not connected
     */
    public boolean isConnected() {
        return this.connection != null && this.connection.isConnected();
    }

    /**
     * Подключается к серверному сокету
     *
     * @return connection
     */
    private Connection connect() throws IOException {
        log.debug("Node [{}]. Attempt to connect to server {}:{}", uuid, host, port);
        Socket socket = new Socket(host, port);
        Connection result = new Connection(socket);
        log.info("Node [{}]. Successful connect to server {}:{}", uuid, host, port);
        return result;
    }

    public Message sendMessage(Message message) throws NetworkExchangeException {
        return sendMessage(message, connectionTimeout);
    }

    public Message sendMessage(Message message, int timeout) throws NetworkExchangeException {
        try {
            return messageExecutor.submit(() -> sendMessageImpl(message)).get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new NetworkExchangeException(e);
        }
    }

    private Message sendMessageImpl(Message message) throws IOException, ClassNotFoundException {
        log.debug("Node [{}]. Client sends message: '{}'", uuid, message);

        connection.send(message);

        return connection.receive();
    }

    @Override
    public void close() {
        log.info("Node [{}]. Close client {}:{}", uuid, host, port);
        if (this.connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}
