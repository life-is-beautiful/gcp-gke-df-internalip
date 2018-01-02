package com.denny.k8s.ws.sample.samplews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class SamplewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SamplewsApplication.class, args);
        System.out.println("Yo! Application Started.");
    }
}
