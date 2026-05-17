package com.notfound.sagaorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BookstoreSagaOrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreSagaOrchestratorServiceApplication.class, args);
    }
}

