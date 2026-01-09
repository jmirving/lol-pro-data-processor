package com.jmirving.prodata.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProDataProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProDataProcessorApplication.class, args);
    }
}
