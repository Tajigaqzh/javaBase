package com.hp.javabase.service.login;

import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.common.utils.email.MailSender;
import com.hp.javabase.common.utils.sms.SmsSender;
import com.hp.javabase.model.enums.VerificationSceneEnum;
import com.hp.javabase.model.enums.VerificationTargetTypeEnum;
import com.hp.javabase.model.vo.VerificationCodeSendVO;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;

/**
 * 验证码服务，负责验证码生成、发送频控、连续输错冻结和验证码校验失效处理。
 *
 * <p>当前阶段手机号验证码已接入短信发送器；邮箱验证码仍仅保留本地风控与调试返回。
 * 开发环境可按配置选择是否把验证码回显到接口响应，便于前后端联调。
 */
@Service
public class VerificationCodeUtils {
    /**
     * 单条验证码有效期，当前按需求固定为 5 分钟。
     */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    /**
     * 同一手机号或邮箱再次发送验证码前的冷却时间。
     */
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);

    /**
     * 小时级发送次数统计窗口。
     */
    private static final Duration HOUR_TTL = Duration.ofHours(1);

    /**
     * 天级发送次数统计窗口。
     */
    private static final Duration DAY_TTL = Duration.ofDays(1);

    /**
     * 验证码连续输错达到阈值后的冻结时长。
     */
    private static final Duration FREEZE_TTL = Duration.ofHours(1);

    /**
     * 单手机号或单邮箱每小时允许发送的最大次数。
     */
    private static final int TARGET_HOUR_LIMIT = 5;

    /**
     * 单手机号或单邮箱每天允许发送的最大次数。
     */
    private static final int TARGET_DAY_LIMIT = 20;

    /**
     * 同一 IP 每小时允许发送的最大次数。
     */
    private static final int IP_HOUR_LIMIT = 20;

    /**
     * 同一 IP 每天允许发送的最大次数。
     */
    private static final int IP_DAY_LIMIT = 100;

    /**
     * 单个验证码对象连续校验失败后触发冻结的次数阈值。
     */
    private static final int VERIFY_FAIL_LIMIT = 3;

    /**
     * Redis 客户端，用于存储验证码、频控计数器和冻结状态。
     */
    private final RedissonClient redissonClient;

    /**
     * 短信发送器，当前用于手机号验证码真实下发。
     */
    private final SmsSender smsSender;

    /**
     * 邮件发送器，当前用于邮箱验证码真实下发。
     */
    private final MailSender mailSender;

    /**
     * 安全随机数生成器，用于创建 6 位数字验证码。
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 是否在接口响应中回显验证码，仅开发联调环境建议开启。
     */
    private final boolean exposeVerificationCodeInResponse;

    /**
     * 构造验证码服务。
     *
     * <p>这里通过构造器注入 Redis 客户端和短信发送器，便于在不同环境下切换真实供应商实现或空实现，
     * 同时通过配置项控制是否在响应中回显验证码，满足本地联调与生产安全的差异化要求。
     *
     * @param redissonClient Redis 客户端
     * @param smsSender 短信发送器
     * @param mailSender 邮件发送器
     * @param exposeVerificationCodeInResponse 是否在响应中回显验证码
     */
    public VerificationCodeUtils(
            RedissonClient redissonClient,
            SmsSender smsSender,
            MailSender mailSender,
            @Value("${app.auth.expose-verification-code-in-response:false}")
            boolean exposeVerificationCodeInResponse) {
        this.redissonClient = redissonClient;
        this.smsSender = smsSender;
        this.mailSender = mailSender;
        this.exposeVerificationCodeInResponse = exposeVerificationCodeInResponse;
    }

    /**
     * 发送手机号验证码，并同步执行单目标、单 IP 的冷却与次数限制。
     *
     * @param phone 手机号
     * @param scene 验证码业务场景
     * @param ip 请求来源 IP
     * @return 发送结果与调试信息
     */
    public VerificationCodeSendVO sendPhoneCode(String phone, VerificationSceneEnum scene, String ip) {
        return sendCode(VerificationTargetTypeEnum.PHONE, phone, scene, ip);
    }

    /**
     * 发送邮箱验证码，并同步执行单目标、单 IP 的冷却与次数限制。
     *
     * @param email 邮箱
     * @param scene 验证码业务场景
     * @param ip 请求来源 IP
     * @return 发送结果与调试信息
     */
    public VerificationCodeSendVO sendEmailCode(String email, VerificationSceneEnum scene, String ip) {
        return sendCode(VerificationTargetTypeEnum.EMAIL, email, scene, ip);
    }

    /**
     * 校验手机号验证码，命中错误次数阈值后冻结该目标 1 小时。
     *
     * @param phone 手机号
     * @param scene 验证码业务场景
     * @param code 待校验验证码
     */
    public void verifyPhoneCode(String phone, VerificationSceneEnum scene, String code) {
        verifyCode(VerificationTargetTypeEnum.PHONE, phone, scene, code);
    }

    /**
     * 校验邮箱验证码，命中错误次数阈值后冻结该目标 1 小时。
     *
     * @param email 邮箱
     * @param scene 验证码业务场景
     * @param code 待校验验证码
     */
    public void verifyEmailCode(String email, VerificationSceneEnum scene, String code) {
        verifyCode(VerificationTargetTypeEnum.EMAIL, email, scene, code);
    }

    /**
     * 统一发送验证码主流程。
     *
     * <p>执行顺序如下：
     * 1. 校验手机号或邮箱目标不能为空；
     * 2. 检查发送冷却时间；
     * 3. 检查该目标是否因连续校验失败被临时冻结；
     * 4. 按目标和 IP 执行小时/天维度限流；
     * 5. 生成验证码并写入 Redis；
     * 6. 如为手机号则通过短信发送器真实下发，如为邮箱则通过邮件发送器真实下发；
     * 7. 返回有效期、冷却时间及可选调试码。
     *
     * @param targetType 验证码目标类型，区分手机号和邮箱
     * @param target 目标值，手机号或邮箱
     * @param scene 业务场景，如注册、登录、找回密码
     * @param ip 请求来源 IP
     * @return 发送结果视图
     */
    private VerificationCodeSendVO sendCode(
            VerificationTargetTypeEnum targetType,
            String target,
            VerificationSceneEnum scene,
            String ip) {
        validateTarget(targetType, target);
        String normalizedIp = normalizeIp(ip);
        String cooldownKey = buildKey("cooldown", targetType, scene, target);
        long cooldownMillis = redissonClient.getBucket(cooldownKey).remainTimeToLive();
        if (cooldownMillis > 0) {
            throw new BusinessException("verification code send is cooling down");
        }
        String freezeKey = buildKey("freeze", targetType, target);
        if (redissonClient.getBucket(freezeKey).isExists()) {
            throw new BusinessException("verification target is frozen");
        }
        incrementCounter(buildKey("target-hour", targetType, scene, target), HOUR_TTL, TARGET_HOUR_LIMIT, "verification code hourly limit reached");
        incrementCounter(buildKey("target-day", targetType, scene, target), DAY_TTL, TARGET_DAY_LIMIT, "verification code daily limit reached");
        incrementCounter(buildKey("ip-hour", targetType, scene, normalizedIp), HOUR_TTL, IP_HOUR_LIMIT, "ip hourly send limit reached");
        incrementCounter(buildKey("ip-day", targetType, scene, normalizedIp), DAY_TTL, IP_DAY_LIMIT, "ip daily send limit reached");

        String code = generateCode();
        redissonClient.getBucket(buildKey("code", targetType, scene, target)).set(code, CODE_TTL);
        redissonClient.getBucket(cooldownKey).set(Boolean.TRUE, SEND_COOLDOWN);
        if (VerificationTargetTypeEnum.PHONE == targetType) {
            smsSender.sendVerificationCode(target, code, CODE_TTL.toMinutes());
        } else if (VerificationTargetTypeEnum.EMAIL == targetType) {
            mailSender.sendVerificationCode(target, code, CODE_TTL.toMinutes());
        }

        return new VerificationCodeSendVO(
                CODE_TTL.toSeconds(),
                SEND_COOLDOWN.toSeconds(),
                exposeVerificationCodeInResponse ? code : null);
    }

    /**
     * 统一校验验证码主流程。
     *
     * <p>校验规则如下：
     * 1. 先校验目标是否为空；
     * 2. 检查目标是否已被冻结；
     * 3. 读取 Redis 中当前场景下的有效验证码；
     * 4. 若验证码匹配，立即删除验证码并清空连续失败计数；
     * 5. 若验证码不匹配，则增加失败次数；
     * 6. 当失败次数达到阈值时冻结该目标 1 小时，并清除旧验证码。
     *
     * @param targetType 验证码目标类型
     * @param target 目标值，手机号或邮箱
     * @param scene 业务场景
     * @param code 待校验验证码
     */
    private void verifyCode(
            VerificationTargetTypeEnum targetType,
            String target,
            VerificationSceneEnum scene,
            String code) {
        validateTarget(targetType, target);
        String freezeKey = buildKey("freeze", targetType, target);
        if (redissonClient.getBucket(freezeKey).isExists()) {
            throw new BusinessException("verification target is frozen");
        }
        String codeKey = buildKey("code", targetType, scene, target);
        String cachedCode = getBucketValue(codeKey);
        if (!StringUtils.hasText(cachedCode)) {
            throw new BusinessException("verification code expired");
        }
        if (Objects.equals(cachedCode, code)) {
            redissonClient.getBucket(codeKey).delete();
            redissonClient.getBucket(buildKey("verify-fail", targetType, target)).delete();
            return;
        }
        RAtomicLong failCounter = redissonClient.getAtomicLong(buildKey("verify-fail", targetType, target));
        long failCount = failCounter.incrementAndGet();
        if (failCount == 1) {
            failCounter.expire(CODE_TTL);
        }
        if (failCount >= VERIFY_FAIL_LIMIT) {
            redissonClient.getBucket(freezeKey).set(Boolean.TRUE, FREEZE_TTL);
            redissonClient.getBucket(codeKey).delete();
            failCounter.delete();
            throw new BusinessException("verification target is frozen");
        }
        throw new BusinessException("verification code is invalid");
    }

    /**
     * 增加某个限流计数器并在首次创建时设置 TTL。
     *
     * <p>该方法用于统一处理目标级、IP 级的小时/天频控逻辑，避免在主流程中重复拼接
     * “自增 + 首次过期 + 超限抛错” 三段式代码。
     *
     * @param key Redis 计数器 key
     * @param ttl 计数器过期时间
     * @param limit 最大允许次数
     * @param message 超限后的错误信息
     */
    private void incrementCounter(String key, Duration ttl, long limit, String message) {
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long current = counter.incrementAndGet();
        if (current == 1) {
            counter.expire(ttl);
        }
        if (current > limit) {
            throw new BusinessException(message);
        }
    }

    /**
     * 校验验证码目标值不能为空。
     *
     * <p>这里根据目标类型返回更准确的错误提示，避免对手机号和邮箱统一返回模糊消息。
     *
     * @param targetType 验证码目标类型
     * @param target 目标值
     */
    private void validateTarget(VerificationTargetTypeEnum targetType, String target) {
        if (!StringUtils.hasText(target)) {
            throw new BusinessException(targetType.getBlankMessagePrefix() + " must not be blank");
        }
    }

    /**
     * 从 Redis 读取字符串验证码。
     *
     * @param key Redis key
     * @return 缓存中的验证码，不存在时返回 null
     */
    private String getBucketValue(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 归一化请求来源 IP。
     *
     * <p>验证码 IP 频控依赖该值作为统计维度，因此当上游未解析到来源 IP 时，
     * 统一降级为 unknown，避免出现空 key。
     *
     * @param ip 原始请求 IP
     * @return 归一化后的 IP 值
     */
    private String normalizeIp(String ip) {
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    /**
     * 生成 6 位数字验证码。
     *
     * @return 固定长度的字符串验证码
     */
    private String generateCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    /**
     * 构造带场景维度的 Redis key。
     *
     * <p>该 key 主要用于验证码正文、发送冷却、按目标计数和按 IP 计数等场景，
     * 结构中保留目标类型和业务场景，避免不同验证码用途之间互相污染。
     *
     * @param prefix 业务前缀，如 code、cooldown、target-hour
     * @param targetType 验证码目标类型
     * @param sceneOrTarget 业务场景或已归一化的 IP
     * @param targetMaybe 手机号或邮箱等目标值
     * @return Redis key
     */
    private String buildKey(
            String prefix,
            VerificationTargetTypeEnum targetType,
            Object sceneOrTarget,
            String targetMaybe) {
        return "auth:verification:" + prefix + ":" + targetType.name() + ":" + sceneOrTarget + ":" + targetMaybe;
    }

    /**
     * 构造不带业务场景的 Redis key。
     *
     * <p>该 key 主要用于冻结状态和连续失败计数，因为这两类状态按“验证码目标对象”维度维护，
     * 不再细分到某个验证码场景。
     *
     * @param prefix 业务前缀，如 freeze、verify-fail
     * @param targetType 验证码目标类型
     * @param target 手机号或邮箱
     * @return Redis key
     */
    private String buildKey(String prefix, VerificationTargetTypeEnum targetType, String target) {
        return "auth:verification:" + prefix + ":" + targetType.name() + ":" + target;
    }
}
