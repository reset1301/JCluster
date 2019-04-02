package ru.rrr.cfg;

public interface Const {
    int PORT_1024 = 1024;
    int PORT_56635 = 65535;

    /**
     * Таймаут отправки сообщений с получением ответа
     */
    int SEND_MESSAGE_TIMEOUT = 5;
    /**
     * Таймаут соединения в секундах по умолчанию
     */
    int CONNECTION_TIMEOUT_DEFAULT = 30;
    /**
     * Периодичность попыток переподключения к другим нодам
     */
    int NODE_DISCOVER_PERIOD = 5;
}
