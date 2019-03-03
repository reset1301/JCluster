package ru.rrr.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageHandler {
    public static String handlePulseMessage(String data) {
        if (data.equalsIgnoreCase("ping"))
            return "PONG";
        log.warn("Пришло сообщение не по фен-шую.");
        return "No ok";
    }
}
