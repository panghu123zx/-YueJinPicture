package com.ph.phpictureback.api;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Configuration
@Component
public class MailConfig {
    @Value("${spring.mail.username}")
    private String username;

    @Resource
    private JavaMailSender mailSender;

    /**
     * 发送邮件
     *
     * @param to      收件人
     * @param subject 邮件标题
     * @param content 内容
     */
    public void sendSimpleMail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(username);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }
}