package ru.rrr.service;

import lombok.Cleanup;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.rrr.client.TCPClient;
import ru.rrr.model.Node;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Data
public class SocketService {
    @Value("${cluster.port}")
    String[] ports;
    @Value("${cluster.ip}")
    String[] ips;
    List<Node> nodes = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (int i = 0; i < ips.length; i++) {
            Node node = new Node();
            int finalI = i;
            new Thread(() -> {
                node.startServer(Integer.parseInt(ports[finalI]));
            }).start();

            for (int j = 0; j < ips.length; j++) {
                if (i == j) {
                    continue;
                }
                TCPClient client = new TCPClient(ips[j], Integer.parseInt(ports[j]));
                node.getClients().add(client);
            }
            node.connectToServers();
            nodes.add(node);
        }
    }

    @Scheduled(fixedRateString = "${cluster.check.status.timeout}")
    public void ping() {
        log.info("Запущена проверка статусов.");
        String message = "PING";
        byte[] messageBytes = message.getBytes();
        nodes.forEach(node -> {
            log.warn("Клиентов на предыдущей итерации: {}", node.getCountClients() + 1);
            node.setCountClients(0);
            node.getClients().forEach(client -> {
                try {
                    new Thread(() -> {
                        try {
                            InputStream inputStream = client.getSocket().getInputStream();
                            node.setCountClients(node.getCountClients() + handleMessageReceive(inputStream));
                        } catch (IOException e) {
                            log.error(e.getMessage());
                        }
                    }).start();
                    log.info("Посылаю сообщение слушателю сокета: " + message);
                    DataOutputStream dataOutputStream = new DataOutputStream(client.getSocket().getOutputStream());
                    dataOutputStream.write(messageBytes);
                    log.info("Сообщение отправлено получателю.");
                } catch (Exception e) {
                    log.error("Ошибка отправки: " + e.getMessage());
                    client.reconnect();
                }
            });
        });
    }

    private int handleMessageReceive(InputStream inputStream) throws IOException {
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

        return receivedMessage.equalsIgnoreCase("PONG") ? 1 : 0;
    }
}

