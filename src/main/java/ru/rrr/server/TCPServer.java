package ru.rrr.server;

import lombok.extern.slf4j.Slf4j;
import ru.rrr.service.MessageHandler;
import ru.rrr.utils.IOHelper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
public class TCPServer {
    /**
     * Порт, на котором поднимается серверный сокет
     */
    private final int port;
    /**
     * Пул потоков для обработки клиентских соединений
     */
    private final ExecutorService exec;

    /**
     * Конструктор
     *
     * @param port порт
     */
    public TCPServer(int port) throws IOException {
        this.port = port;
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
    public void startServer(int port) throws IOException {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                log.info("Waiting for a new client...");
                try (final Socket fromClient = serverSocket.accept()) {
                    log.info("Current client connected {}:{}", fromClient.getInetAddress().getHostAddress(),
                            fromClient.getPort());
                    exec.submit(() -> runClient(fromClient));
                } catch (IOException e) {
                    log.warn("Can't accept");
                    throw e;
                }
            }
        } catch (IOException e) {
            log.info("Couldn't listen to port {}", port);
            throw e;
        }
    }

    /**
     * Обмен данными с клиентом по TCP
     *
     * @param fromClient сокет от клиента
     */
    private void runClient(Socket fromClient) {
        Thread.currentThread().setDaemon(true);
        while (true) {
            try {
                DataInputStream in = new DataInputStream(fromClient.getInputStream());
                DataOutputStream out = new DataOutputStream(fromClient.getOutputStream());

                String data = in.readUTF();
                log.info("Сервером с портом {} получено сообщение: " + data, port);
                String response = MessageHandler.handlePulseMessage(data);
                out.writeUTF(response);
                out.flush();
                log.info("Отправляю сообщение: " + response);
            } catch (Exception e) {
                log.info("Current client disconnected");
                log.info(e.toString());
                break;
            }
        }
    }
}
