package ru.rrr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication(scanBasePackages = "ru.rrr")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
