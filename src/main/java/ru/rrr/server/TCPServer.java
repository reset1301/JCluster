package ru.rrr.server;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.rrr.service.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
@Component
@Data
public class TCPServer {
    int port;

    public void startServer(int port) {
        this.port = port;
        ServerSocket servers = null;
        Socket fromclient;
        try {
            servers = new ServerSocket(port);
        } catch (IOException e) {
            log.info("Couldn't listen to port " + port);
            System.exit(-1);
        }
        while (true) {
            try {
                log.info("Waiting for a new client...");
                fromclient = servers.accept();
                Socket finalFromclient = fromclient;

                Thread thread = new Thread(() -> runClient(finalFromclient));
                thread.setDaemon(true);
                thread.start();

                log.info("Current client connected");
                if (servers.isClosed()) {
                    servers = new ServerSocket(port);
                }
            } catch (IOException e) {
                log.info("Can't accept");
                System.exit(-1);
            }
        }
    }

    private void runClient(Socket fromclient) {
        while (true) {
            try {
                InputStream inputStream = fromclient.getInputStream();
                OutputStream outputStream = fromclient.getOutputStream();
                byte[] buf = new byte[64 * 1024];
                int r = inputStream.read(buf);
                String data = new String(buf, 0, r);
                log.info("Сервером с портом {} получено сообщение: " + data, port);
                String response = new Handler().handleMessage(data);
                final int lengthMessage = String.valueOf(response).getBytes().length;
                outputStream.write(String.valueOf(response).getBytes());
                log.info("Отправляю сообщение: " + response);
            } catch (Exception e) {
                log.info("Current client disconnected");
                log.info(e.toString());
                break;
            }
        }
    }
}
