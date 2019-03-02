package ru.rrr.cfg;

/**
 * Конфигурация ноды кластера
 */
public class NodeConfig {
    /**
     * Таймаут соединения в секундах по умолчанию
     */
    public static final int CONNECTION_TIMEOUT_DEFAULT = 30;

    private String host;
    private int port;

    /**
     * Таймаут соединения с другими нодами кластера в секундах
     */
    private int connectionTimeoutSeconds;
    /**
     * Периодичность обнаружения/пинга других нод кластера
     */
    private int nodesDiscoverPeriod;

}
