package ru.rrr.service;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.rrr.client.TCPClient;
import ru.rrr.model.Node;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@Scope("singleton")
public class ClientSocketFactory {
    @Autowired
    private ApplicationEventPublisher publisher;
    @Value("${cluster.port}")
    Integer[] ports;
    @Value("${cluster.ip}")
    String[] ips;
    List<Node> nodes = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (int i = 0; i < ips.length; i++) {
            Node node = new Node();
            int finalI = i;
            new Thread(() -> {
                node.startServer(ports[finalI]);
            }).start();

            for (int j = 0; j < ips.length; j++) {
                if (i == j) {
                    continue;
                }
                TCPClient client = new TCPClient(ips[j], ports[j]);
                node.getClients().add(client);
            }
            node.connectToServers();
            nodes.add(node);
        }
    }

    @Scheduled(fixedRateString = "3000")
    public void ping() {
        String message = "PING";
        byte[] messageBytes = message.getBytes();
        for (Node node : nodes) {
            for (TCPClient client : node.getClients()) {
                try {
                    new Thread(() -> {
                        try {
                            InputStream inputStream = client.getSocket().getInputStream();
                            handleMessageReceive(inputStream);
                        } catch (IOException e) {
                            log.error(e.getMessage());
                        }
                    }).start();
                    log.info("Посылаю сообщение слушателю сокета: \n" + message);
                    DataOutputStream dataOutputStream = new DataOutputStream(client.getSocket().getOutputStream());
                    dataOutputStream.write(messageBytes);
                    log.info("Сообщение отправлено получателю.");
                } catch (Exception e) {
                    log.error("Ошибка отправки: " + e.getMessage());
                    client.reconnect();
                }
            }

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

        this.publisher.publishEvent(receivedMessage);
    }
}

