package ru.rrr;

import org.junit.Ignore;
import org.junit.Test;
import ru.rrr.cfg.NodeConfig;
import ru.rrr.cfg.NodeConfigException;
import ru.rrr.cluster.Node;
import ru.rrr.tcp.TcpClient;
import ru.rrr.tcp.TcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Unit test for simple App.
 */
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
//        final Node node1 = new Node(nodeConfig);

        TimeUnit.MINUTES.sleep(3);
    }

    @Test
    public void testStartServerAndClient() throws IOException {
        final TcpServer tcpServer = new TcpServer(1024, "uuid", "test");

        final TcpClient tcpClient = new TcpClient("localhost", 1024, "uuid", 5, 5);

    }
}
