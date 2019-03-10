package ru.rrr.model;

import java.io.Serializable;

public class Message implements Serializable {
    private final MessageType type;
    private final String data;

    public Message(MessageType messageType) {
        this.type = messageType;
        this.data = null;
    }

    public Message(MessageType messageType, String content) {
        this.type = messageType;
        this.data = content;
    }

    public MessageType getType() {
        return type;
    }

    public String getData() {
        return data;
    }
}
