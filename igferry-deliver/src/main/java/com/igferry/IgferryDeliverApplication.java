package com.igferry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class IgferryDeliverApplication {

    public static void main(String[] args) {
        SpringApplication.run(IgferryDeliverApplication.class, args);
    }

}
