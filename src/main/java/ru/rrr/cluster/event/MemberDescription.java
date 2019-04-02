package ru.rrr.cluster.event;

import ru.rrr.tcp.TcpClient;

/**
 * Описание члена кластера
 */
public class MemberDescription {
    private final String uuid;
    private final String host;
    private final int port;

    public MemberDescription(String uuid, String host, int port) {
        this.uuid = uuid;
        this.host = host;
        this.port = port;
    }

    public MemberDescription(TcpClient tcpClient) {
        this.uuid = tcpClient.getUuid();
        this.host = tcpClient.getHost();
        this.port = tcpClient.getPort();
    }

    public String getUuid() {
        return uuid;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "MemberDescription{" +
                "uuid='" + uuid + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
