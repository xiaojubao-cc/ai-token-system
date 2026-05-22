package com.ai.system.util.mail;

import jakarta.annotation.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailUtil {

    @Resource
    private JavaMailSender mailSender;

    public void sendPasswordReset(String to, String username, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("密码重置通知 - AI Token 系统");
        message.setText(String.format("""
                尊敬的 %s：

                您的密码已重置成功，以下是您的新密码：

                    %s

                请尽快登录系统并修改密码。

                此邮件由系统自动发送，请勿回复。
                """, username, newPassword));
        mailSender.send(message);
    }

    public void sendTestMail(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("测试邮件 - AI Token 系统");
        message.setText("""
                您好：

                这是一封来自 AI Token 系统的测试邮件。

                如果您收到此邮件，说明邮件发送功能配置正常。

                此邮件由系统自动发送，请勿回复。
                """);
        mailSender.send(message);
    }
}
