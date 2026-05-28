package com.hp.javabase.common.utils.email;

import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.config.SmtpMailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件发送器抽象，隔离验证码服务与具体邮件发送实现。
 */
public interface MailSender {

    /**
     * 发送邮箱验证码。
     *
     * @param email 收件邮箱
     * @param code 验证码
     * @param expireMinutes 有效分钟数
     */
    void sendVerificationCode(String email, String code, long expireMinutes);


}
