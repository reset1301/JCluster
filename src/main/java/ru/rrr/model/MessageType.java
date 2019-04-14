package ru.rrr.model;

public enum MessageType {
    /**
     * Запрос UUID ноды
     */
    GET_UUID,
    /**
     * Запрос имени кластера, в котором состоит нода
     */
    GET_CLUSTER_NAME,
    /**
     * Запрос на закрытие соединения
     */
    CLOSE_CONNECTION,
    /**
     * Текстовое сообщение
     */
    TEXT
}
