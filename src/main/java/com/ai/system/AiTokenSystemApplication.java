package com.ai.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.ai.system.config")
public class AiTokenSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiTokenSystemApplication.class, args);
    }
}
