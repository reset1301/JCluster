package ru.rrr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.rrr.model.Node;

/**
 * Hello world!
 */
@Slf4j
//@SpringBootApplication
public class App {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);

        log.info("Start servers...");
//        context.getBean(TCPServer.class).startServer(context);
        Node node1 = new Node();
        Node node2 = new Node();
        new Thread(() -> {
            node1.startServer(4441);
        }).start();
        new Thread(() -> {
            node2.startServer(4442);
        }).start();
        new Thread(() -> node2.connectToServer("localhost", 4441)).start();
        new Thread(() -> node1.connectToServer("localhost", 4442)).start();
    }
}
