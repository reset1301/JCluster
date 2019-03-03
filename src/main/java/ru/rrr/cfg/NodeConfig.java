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
     * Таймаут соединения в секундах по умолчанию
     */
    private static final int CONNECTION_TIMEOUT_DEFAULT = 30;

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

    public NodeConfig() {
    }

    public NodeConfig(String configPath) throws IOException, NodeConfigException {
        Properties properties = new Properties();
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(configPath)) {
            properties.load(is);
        }
        final String membersURI = properties.getProperty("members");
        if (membersURI == null || membersURI.isEmpty()) {
            throw new NodeConfigException("Property 'members' is required, but not found. " +
                    "Property must match the 'host1:port1, host2:port2, ..., hostN:portN' pattern");
        }

        this.members = new ArrayList<>();

        final String[] split = membersURI.split(",");
        for (String uri : split) {
            final String[] uriSplit = uri.trim().split(":");
            if (uriSplit.length < 2) {
                throw new NodeConfigException("Incorrect members URI format. URI must match the 'host:port' pattern");
            }

            this.members.add(new NodeUri(uriSplit[0], Integer.valueOf(uriSplit[1])));

            this.connectionTimeoutSeconds = Integer.valueOf(properties.getProperty("connection-timeout-seconds",
                    String.valueOf(CONNECTION_TIMEOUT_DEFAULT)));
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
}
