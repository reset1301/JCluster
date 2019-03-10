package ru.rrr.cfg;

/**
 * URI ноды в сети
 */
public class NodeUri {
    private String host;
    private int port;

    public NodeUri() {
    }

    public NodeUri(String host) {
        this.host = host;
        this.port = 0;
    }

    public NodeUri(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
