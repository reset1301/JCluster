package ru.rrr.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.Socket;

@Slf4j
@Data
public class TCPClient implements AutoCloseable {
    private final int port;
    private final String ip;
    @JsonIgnore
    private Socket socket = null;
    @Value("${node.reconnect.timeout}")
    @JsonIgnore
    private Integer reconnectTimeout;

    public TCPClient(String ip, int port) {
        this.port = port;
        this.ip = ip;

    }

    public Socket getSocket() {
        return this.socket;
    }

    /**
     * Подключается к серверному сокету
     *
     * @return
     */
    public boolean connect() {
        log.info("Попытка подключения к серверу {}:{}", ip, port);
        try {
            socket = new Socket(ip, port);
            log.info("Успешное подключение к серверу - {}:{}", ip, port);
        } catch (Exception e) {
            log.warn("Не удалось подключиться к серверу " + ip + ":" + port + "", e);
            return false;
        }

        return true;
    }

    @Override
    public void close() {
        try {
            getSocket().close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
