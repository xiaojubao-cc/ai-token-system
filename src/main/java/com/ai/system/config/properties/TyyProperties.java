package com.ai.system.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tyy-properties")
public class TyyProperties {
    private String accessKey;
    private String securityKey;
    private String baseUrl;
    private String apikeyUrl;
    private String tokenUrl;
}
