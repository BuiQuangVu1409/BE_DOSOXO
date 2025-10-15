package com.example.doxoso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@EnableJpaAuditing
@SpringBootApplication
public class DoxosoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoxosoApplication.class, args);
    }
}



