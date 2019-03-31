package ru.rrr;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import ru.rrr.cfg.NodeConfig;
import ru.rrr.cfg.NodeConfigException;
import ru.rrr.cluster.Node;
import ru.rrr.model.Message;
import ru.rrr.model.MessageType;
import ru.rrr.tcp.Connection;
import ru.rrr.tcp.TcpClient;
import ru.rrr.tcp.TcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
@Slf4j
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Ignore
    @Test
    public void testStartCluster() throws IOException, NodeConfigException, InterruptedException {
        final NodeConfig nodeConfig = new NodeConfig("cfg/ClusterConfig.properties");
        final Node node = new Node(nodeConfig);
        final Node node1 = new Node(nodeConfig);
//        final Node node3 = new Node(nodeConfig);

        TimeUnit.SECONDS.sleep(20);
    }

    @Ignore
    @Test
    public void testStartServerAndClient() throws IOException, InterruptedException {
        final int port = 1024;
        final TcpServer tcpServer = new TcpServer(port, "uuid", "test");

        final TcpClient tcpClient = new TcpClient("localhost", port, "uuid", 5, 5);


        final Socket socket = new Socket("localhost", port);
        assertTrue(socket.isConnected());

//        final Connection connection = new Connection(socket);

//        assertTrue(connection.isConnected());

        TimeUnit.MINUTES.sleep(5);

        socket.close();
        tcpServer.close();
    }

    @Ignore
    @Test
    public void runTcpClient() throws InterruptedException, IOException {
        final String host = "localhost";
        final int port = 1024;

        final AtomicBoolean isRunning = new AtomicBoolean(true);

        final ServerSocket serverSocket = new ServerSocket(port);

        final Thread serverThread = new Thread(() -> {
            log.info("Start server");
            while (isRunning.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    final Thread serverHandShake = new Thread(() -> {
                        try {
                            log.info("Server: new client connected {}:{}", socket.getInetAddress(), socket.getPort());
                            final Connection connection = new Connection(socket);
                            final Message message = new Message(MessageType.GET_CLUSTER_NAME);
                            log.info("Server: send message {}", message);
                            connection.send(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    serverHandShake.setDaemon(true);
                    serverHandShake.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            log.info("Stop server");
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Socket client = new Socket(host, port);

        Thread clientThread = new Thread(() -> {
            try {
                final Connection connection = new Connection(client);
                while (isRunning.get()) {
                    final Message receive = connection.receive();
                    log.info("Client: receive message: {}:{}", receive.getType(), receive.getData());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();

        TimeUnit.MINUTES.sleep(1);

        isRunning.set(false);
    }
}
