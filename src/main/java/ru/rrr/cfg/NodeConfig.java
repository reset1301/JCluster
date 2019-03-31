package ru.rrr.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Конфигурация ноды кластера
 */
public class NodeConfig {

    /**
     * Периодичность попыток переподключения к другим нодам
     */
    private static final int NODE_DISCOVER_PERIOD = 5;

    /**
     * Таймаут соединения с другими нодами кластера в секундах
     */
    private int connectionTimeoutSeconds;

    /**
     * Периодичность обнаружения/пинга других нод кластера
     */

    private int nodesDiscoverPeriod;

    /**
     * Список URI нод кластера
     */
    private Collection<NodeUri> members;

    /**
     * Порт по умолчанию, на котором стартует нода
     */
    private int port;
    private String clusterName;

    public NodeConfig() {
    }

    public NodeConfig(String configPath) throws IOException, NodeConfigException {
        Properties properties = new Properties();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(configPath)) {
            properties.load(is);
        }

        this.port = Integer.valueOf(properties.getProperty("port"));
        this.clusterName = properties.getProperty("cluster-name");

        final String membersURI = properties.getProperty("members");
        if (membersURI == null || membersURI.isEmpty()) {
            throw new NodeConfigException("Property 'members' is required, but not found. " +
                    "Property must match the 'host1:port1, host2:port2, ..., hostN:portN' pattern");
        }

        this.members = new ArrayList<>();

        final String[] split = membersURI.split(",");
        for (String uri : split) {
            final NodeUri nodeUri = new NodeUri();
            final String[] uriSplit = uri.trim().split(":");
            switch (uriSplit.length) {
                case 1:
                    nodeUri.setHost(uriSplit[0]);
                    break;
                case 2:
                    nodeUri.setPort(Integer.valueOf(uriSplit[1]));
                    break;
                default:
                    throw new NodeConfigException("Incorrect members URI format. " +
                            "URI must match the 'host:port' or 'host' pattern");
            }

            this.members.add(new NodeUri(uriSplit[0], uriSplit.length > 1 ? Integer.valueOf(uriSplit[1]) : 0));

            this.connectionTimeoutSeconds = Integer.valueOf(properties.getProperty("connection-timeout-seconds",
                    String.valueOf(Const.CONNECTION_TIMEOUT_DEFAULT)));
            this.nodesDiscoverPeriod = Integer.valueOf(properties.getProperty("nodes-discover-period-seconds",
                    String.valueOf(NODE_DISCOVER_PERIOD)));
        }
    }

    public Collection<NodeUri> getMembers() {
        return members;
    }

    public void setMembers(List<NodeUri> members) {
        this.members = members;
    }

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public int getNodesDiscoverPeriod() {
        return nodesDiscoverPeriod;
    }

    public void setNodesDiscoverPeriod(int nodesDiscoverPeriod) {
        this.nodesDiscoverPeriod = nodesDiscoverPeriod;
    }

    public int getPort() {
        return port;
    }

    public String getClusterName() {
        return clusterName;
    }
}
