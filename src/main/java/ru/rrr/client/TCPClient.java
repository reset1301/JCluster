package ru.rrr.client;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class TCPClient {
    private int port;
    private String ip;
    private Socket socket = null;
    private Integer reconnectTimeout = 10000; // как часто пытаемся переподключиться

    @PostConstruct
    public void init() {
    }

    public TCPClient( String ip,int port) {
        this.port = port;
        this.ip = ip;
    }

    /**
     * Коннектится к серверам до скончания времен
     *
     */
    @SneakyThrows
    public void reconnect() {
        boolean connected = this.connect();
        if (!connected) {
            TimeUnit.MILLISECONDS.sleep(reconnectTimeout);
            reconnect();
            return;
        }

        log.info("Успешное подключение к серверу - " + ip + ":" + port);

    }

    public Socket getSocket() {
        if (this.socket == null) {
            init();
            return this.socket;
        }
        return this.socket;
    }

    public void closeSocket() {
        try {
            getSocket().close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Производит одну попытку подключения
     *
     * @return
     */
    public boolean connect() {
        try {
            socket = new Socket(ip, port);
        } catch (
          Exception e) {
            log.warn(e.getMessage());
            return false;
        }

        return true;
    }
}
