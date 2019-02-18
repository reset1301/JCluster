package ru.rrr.service;

import org.springframework.stereotype.Component;

@Component
public class Handler {
    public String handleMessage(String data) {
        return "OK";
    }
}
