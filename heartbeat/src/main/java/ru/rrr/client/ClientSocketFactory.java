package ru.rrr.client;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

//@Component
@Slf4j
@Scope("singleton")
public class ClientSocketFactory {
    @Autowired
    private ApplicationEventPublisher publisher;

    private Integer reconnectTimeout = 10000; // как часто пытаемся переподключиться
    private Socket socket = null;

    @PostConstruct
    public void init() {
//        this.reconnect(ip, port);
    }

    /**
     * Коннектится к серверам до скончания времен
     *
     * @param ip
     * @param port
     */
    @SneakyThrows
    public void reconnect(String ip, int port) {
        boolean connected = this.connect(ip, port);
        if (!connected) {
            TimeUnit.MILLISECONDS.sleep(reconnectTimeout);
            reconnect(ip, port);
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
    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
        } catch (
          Exception e) {
            log.warn(e.getMessage());
            return false;
        }

        return true;
    }

    @Scheduled
    public void ping() {
        String message = "ping ";
        byte[] messageBytes = message.getBytes();
        try {
            new Thread(() -> {
                try {
                    InputStream inputStream = this.getSocket().getInputStream();
                    handleMessageReceive(inputStream);
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }).start();

            log.info("Посылаю сообщение слушателю сокета: \n" + message);
            DataOutputStream dataOutputStream = new DataOutputStream(getSocket().getOutputStream());
            dataOutputStream.write(messageBytes);
            log.info("В очереди осталось:" + messageQueue.size());
        } catch (Exception e) {
            log.error("Ошибка отправки: " + e.getMessage());
            kztaSocketFactory.reconnect();
        }
    }

    private void handleMessageReceive(InputStream inputStream) throws IOException {
        String receivedMessage = "";
        @Cleanup ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int size = 13 * 1024;
        byte[] responseBytes = new byte[size];

        int readSize;
        while ((readSize = inputStream.read(responseBytes, 0, responseBytes.length)) > 0) {
            baos.write(responseBytes, 0, readSize);
            if (readSize < size) {
                break;
            }
        }

        receivedMessage = new String(baos.toByteArray(), "UTF-8");
        log.info("Получено сообщение: {}", receivedMessage);
        MessageReceivedEvent event = new MessageReceivedEvent(receivedMessage);
        this.publisher.publishEvent(event);
    }
}

