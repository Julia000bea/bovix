package com.bovix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BovixApplication {
    public static void main(String[] args) {
        SpringApplication.run(BovixApplication.class, args);
    }
}
