package ru.rrr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Handler {
    public String handleMessage(String data) {
        if (data.equalsIgnoreCase("ping"))
            return "PONG";
        log.warn("Пришло сообщение не по фен-шую.");
        return "No ok";
    }
}
