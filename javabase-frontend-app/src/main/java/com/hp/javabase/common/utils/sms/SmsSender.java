package com.hp.javabase.common.utils.sms;

/**
 * 短信发送器抽象，隔离验证码服务与具体短信供应商 SDK 的耦合。
 */
public interface SmsSender {

    /**
     * 发送短信验证码。
     *
     * @param phone 手机号
     * @param code 验证码
     * @param expireMinutes 过期分钟数
     */
    void sendVerificationCode(String phone, String code, long expireMinutes);
}
