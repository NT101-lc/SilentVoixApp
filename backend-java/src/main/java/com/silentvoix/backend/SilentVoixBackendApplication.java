package com.silentvoix.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SilentVoixBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilentVoixBackendApplication.class, args);
    }
}
