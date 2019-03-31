package ru.rrr.cluster.event;

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

    public String getUuid() {
        return uuid;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
