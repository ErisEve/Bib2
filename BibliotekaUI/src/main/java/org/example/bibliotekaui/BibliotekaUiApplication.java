package org.example.bibliotekaui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class BibliotekaUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliotekaUiApplication.class, args);
    }

}
