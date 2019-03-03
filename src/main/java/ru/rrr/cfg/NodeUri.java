package ru.rrr.cfg;

/**
 * URI ноды в сети
 */
public class NodeUri {
    private final String host;
    private final int port;

    public NodeUri(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
