package com.hp.javabase.common.utils.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 默认邮件发送器，在未启用真实 SMTP 时只记录日志，不对外发送邮件。
 */
@Service
public class NoopMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(NoopMailSender.class);

    @Override
    public void sendVerificationCode(String email, String code, long expireMinutes) {
        log.info("mail sender disabled, skip sending verify code to email={}, expireMinutes={}", email, expireMinutes);
    }
}