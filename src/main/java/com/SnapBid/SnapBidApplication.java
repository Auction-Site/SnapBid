package com.SnapBid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SnapBidApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapBidApplication.class, args);
    }
}
