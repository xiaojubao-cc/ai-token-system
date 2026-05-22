package com.ai.system;

import com.ai.system.config.properties.TyyProperties;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AiTokenSystemApplicationTest {

    @Resource
    private TyyProperties tyyProperties;

    @Test
    public void contextLoads() {
        System.out.println(tyyProperties.getAccessKey());
    }

    @Test
    public void sendTestMail() {

    }
}
