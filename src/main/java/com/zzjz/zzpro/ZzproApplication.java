package com.zzjz.zzpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZzproApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZzproApplication.class, args);
    }
}
