package com.project.bitb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.project.bitb")
public class BitbApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitbApplication.class, args);
    }
}
