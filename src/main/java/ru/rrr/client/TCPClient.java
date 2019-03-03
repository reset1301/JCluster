package ru.rrr.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

// TODO: 03.03.2019 Вынести общий функционал в интерфейс/абстрактный класс типа ClusterClient с разными имплементациями
@Slf4j
@Data
public class TCPClient implements AutoCloseable {
    private final String host;
    private final int port;
    private Socket socket;
    private final int reconnectTimeout;
    private final int connectionTimeout;

    public TCPClient(String host, int port, int connectionTimeout, int reconnectTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.reconnectTimeout = reconnectTimeout;
        try {
            this.socket = connect();
        } catch (IOException e) {
            log.error("Не удалось подключиться к серверу {}:{}", host, port, e);
        }
    }

    public Socket getSocket() {
        return this.socket;
    }


    /**
     * Подключается к серверному сокету
     *
     * @return сокет
     */
    public Socket connect() throws IOException {
        log.info("Попытка подключения к серверу {}:{}", host, port);
        try {
            Socket socket = new Socket(host, port);
            log.info("Успешное подключение к серверу {}:{}", host, port);
            return socket;
        } catch (Exception e) {
            log.error("Не удалось подключиться к серверу {}:{}", host, port, e);
            throw e;
        }
    }

    public String sendMessage(String message) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeUTF(message);
        out.flush();

        return in.readUTF();
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
