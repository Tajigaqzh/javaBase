package com.hp.javabase.common.utils.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认短信发送器，在未启用真实短信供应商时作为兜底 Bean，避免应用因缺少实现而启动失败。
 */
public class NoopSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(NoopSmsSender.class);

    @Override
    public void sendVerificationCode(String phone, String code, long expireMinutes) {
        log.warn("sms sender disabled, skip sending verify code to phone={}, expireMinutes={}", phone, expireMinutes);
    }
}
