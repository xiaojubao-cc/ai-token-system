package com.ai.system;

import com.ai.system.config.properties.TyyProperties;
import com.ai.system.service.MailService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AiTokenSystemApplicationTest {

    @Resource
    private TyyProperties tyyProperties;

    @Resource
    private MailService mailService;

    @Test
    public void contextLoads() {
        System.out.println(tyyProperties.getAccessKey());
    }

    @Test
    public void sendTestMail() {
        mailService.sendTestMail("1171264943@qq.com");
        System.out.println("测试邮件发送成功");
    }
}
