package com.hp.javabase.service.login;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.model.enums.VerificationSceneEnum;
import com.hp.javabase.model.enums.VerificationTargetTypeEnum;
import com.hp.javabase.model.vo.VerificationCodeSendVO;
import com.hp.javabase.common.utils.email.MailSender;
import com.hp.javabase.common.utils.sms.SmsSender;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

/**
 * {@link VerificationCodeUtils} 单元测试，覆盖验证码发送、频控、校验成功和连续输错冻结等核心行为。
 */
class VerificationTests {

    @Test
    void sendPhoneCodeShouldStoreCodeAndDelegateToSmsSender() {
        FakeRedisSupport fakeRedisSupport = new FakeRedisSupport();
        RedissonClient redissonClient = fakeRedisSupport.createClient();
        SmsSender smsSender = Mockito.mock(SmsSender.class);
        MailSender mailSender = Mockito.mock(MailSender.class);
        VerificationCodeUtils service = new VerificationCodeUtils(redissonClient, smsSender, mailSender, true);

        VerificationCodeSendVO response = service.sendPhoneCode(
                "15939654984",
                VerificationSceneEnum.REGISTER,
                "127.0.0.1");

        assertEquals(300L, response.getExpireSeconds());
        assertEquals(60L, response.getCooldownSeconds());
        assertNotNull(response.getDebugCode());
        assertEquals(6, response.getDebugCode().length());
        verify(smsSender).sendVerificationCode("15939654984", response.getDebugCode(), 5L);
        assertEquals(
                response.getDebugCode(),
                fakeRedisSupport.getValue(codeKey(VerificationTargetTypeEnum.PHONE, VerificationSceneEnum.REGISTER, "15939654984")));
    }

    @Test
    void sendEmailCodeShouldStoreCodeAndDelegateToMailSender() {
        FakeRedisSupport fakeRedisSupport = new FakeRedisSupport();
        RedissonClient redissonClient = fakeRedisSupport.createClient();
        SmsSender smsSender = Mockito.mock(SmsSender.class);
        MailSender mailSender = Mockito.mock(MailSender.class);
        VerificationCodeUtils service = new VerificationCodeUtils(redissonClient, smsSender, mailSender, true);

        VerificationCodeSendVO response = service.sendEmailCode(
                "201267151@qq.com",
                VerificationSceneEnum.FORGOT_PASSWORD,
                "127.0.0.1");

        assertNotNull(response.getDebugCode());
        verify(mailSender).sendVerificationCode("201267151@qq.com", response.getDebugCode(), 5L);
        assertEquals(
                response.getDebugCode(),
                fakeRedisSupport.getValue(codeKey(VerificationTargetTypeEnum.EMAIL, VerificationSceneEnum.FORGOT_PASSWORD, "201267151@qq.com")));
    }

    @Test
    void sendPhoneCodeShouldRejectRequestDuringCooldown() {
        FakeRedisSupport fakeRedisSupport = new FakeRedisSupport();
        fakeRedisSupport.putValue(
                cooldownKey(VerificationTargetTypeEnum.PHONE, VerificationSceneEnum.LOGIN, "15939654984"),
                Boolean.TRUE,
                Duration.ofSeconds(30).toMillis());
        VerificationCodeUtils service = new VerificationCodeUtils(
                fakeRedisSupport.createClient(),
                Mockito.mock(SmsSender.class),
                Mockito.mock(MailSender.class),
                true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendPhoneCode("15939654984", VerificationSceneEnum.LOGIN, "127.0.0.1"));

        assertEquals("verification code send is cooling down", exception.getMessage());
    }

    @Test
    void verifyPhoneCodeShouldSucceedAndInvalidateCachedCode() {
        FakeRedisSupport fakeRedisSupport = new FakeRedisSupport();
        fakeRedisSupport.putValue(
                codeKey(VerificationTargetTypeEnum.PHONE, VerificationSceneEnum.LOGIN, "15939654984"),
                "123456",
                Duration.ofMinutes(5).toMillis());
        fakeRedisSupport.getAtomicCounter(failKey(VerificationTargetTypeEnum.PHONE, "15939654984")).set(2);
        VerificationCodeUtils service = new VerificationCodeUtils(
                fakeRedisSupport.createClient(),
                Mockito.mock(SmsSender.class),
                Mockito.mock(MailSender.class),
                true);

        assertDoesNotThrow(() -> service.verifyPhoneCode("15939654984", VerificationSceneEnum.LOGIN, "123456"));

        assertFalse(fakeRedisSupport.exists(codeKey(VerificationTargetTypeEnum.PHONE, VerificationSceneEnum.LOGIN, "15939654984")));
        assertFalse(fakeRedisSupport.exists(failKey(VerificationTargetTypeEnum.PHONE, "15939654984")));
    }

    @Test
    void verifyEmailCodeShouldFreezeTargetAfterThreeFailures() {
        FakeRedisSupport fakeRedisSupport = new FakeRedisSupport();
        fakeRedisSupport.putValue(
                codeKey(VerificationTargetTypeEnum.EMAIL, VerificationSceneEnum.FORGOT_PASSWORD, "201267151@qq.com"),
                "123456",
                Duration.ofMinutes(5).toMillis());
        VerificationCodeUtils service = new VerificationCodeUtils(
                fakeRedisSupport.createClient(),
                Mockito.mock(SmsSender.class),
                Mockito.mock(MailSender.class),
                true);

        assertEquals(
                "verification code is invalid",
                assertThrows(
                        BusinessException.class,
                        () -> service.verifyEmailCode("201267151@qq.com", VerificationSceneEnum.FORGOT_PASSWORD, "000001"))
                        .getMessage());
        assertEquals(
                "verification code is invalid",
                assertThrows(
                        BusinessException.class,
                        () -> service.verifyEmailCode("201267151@qq.com", VerificationSceneEnum.FORGOT_PASSWORD, "000002"))
                        .getMessage());
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.verifyEmailCode("201267151@qq.com", VerificationSceneEnum.FORGOT_PASSWORD, "000003"));

        assertEquals("verification target is frozen", exception.getMessage());
        assertTrue(fakeRedisSupport.exists(freezeKey(VerificationTargetTypeEnum.EMAIL, "201267151@qq.com")));
        assertFalse(fakeRedisSupport.exists(codeKey(VerificationTargetTypeEnum.EMAIL, VerificationSceneEnum.FORGOT_PASSWORD, "201267151@qq.com")));
    }

    private static String codeKey(VerificationTargetTypeEnum targetType, VerificationSceneEnum scene, String target) {
        return "auth:verification:code:" + targetType.name() + ":" + scene + ":" + target;
    }

    private static String cooldownKey(VerificationTargetTypeEnum targetType, VerificationSceneEnum scene, String target) {
        return "auth:verification:cooldown:" + targetType.name() + ":" + scene + ":" + target;
    }

    private static String freezeKey(VerificationTargetTypeEnum targetType, String target) {
        return "auth:verification:freeze:" + targetType.name() + ":" + target;
    }

    private static String failKey(VerificationTargetTypeEnum targetType, String target) {
        return "auth:verification:verify-fail:" + targetType.name() + ":" + target;
    }

    /**
     * 轻量级 Redis mock 支撑，使用内存 Map 模拟验证码、TTL 和计数器状态，避免单元测试依赖真实 Redis。
     */
    static final class FakeRedisSupport {

        private final Map<String, Object> bucketValues = new ConcurrentHashMap<>();
        private final Map<String, Long> ttlMillis = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> atomicCounters = new ConcurrentHashMap<>();

        RedissonClient createClient() {
            RedissonClient redissonClient = Mockito.mock(RedissonClient.class);
            Mockito.when(redissonClient.getBucket(anyString())).thenAnswer(invocation -> createBucket(invocation.getArgument(0)));
            Mockito.when(redissonClient.getAtomicLong(anyString())).thenAnswer(invocation -> createAtomicLong(invocation.getArgument(0)));
            return redissonClient;
        }

        private RBucket<Object> createBucket(String key) {
            @SuppressWarnings("unchecked")
            RBucket<Object> bucket = Mockito.mock(RBucket.class);
            Mockito.doAnswer(invocation -> {
                bucketValues.put(key, invocation.getArgument(0));
                ttlMillis.remove(key);
                return null;
            }).when(bucket).set(any());
            Mockito.doAnswer(invocation -> {
                bucketValues.put(key, invocation.getArgument(0));
                Duration ttl = invocation.getArgument(1);
                ttlMillis.put(key, ttl.toMillis());
                return null;
            }).when(bucket).set(any(), any(Duration.class));
            Mockito.when(bucket.get()).thenAnswer(invocation -> bucketValues.get(key));
            Mockito.when(bucket.delete()).thenAnswer(invocation -> {
                boolean existed = bucketValues.containsKey(key)
                        || ttlMillis.containsKey(key)
                        || atomicCounters.containsKey(key);
                bucketValues.remove(key);
                ttlMillis.remove(key);
                atomicCounters.remove(key);
                return existed;
            });
            Mockito.when(bucket.isExists()).thenAnswer(invocation ->
                    bucketValues.containsKey(key) || atomicCounters.containsKey(key));
            Mockito.when(bucket.remainTimeToLive()).thenAnswer(invocation -> ttlMillis.getOrDefault(key, -1L));
            return bucket;
        }

        private RAtomicLong createAtomicLong(String key) {
            RAtomicLong atomicLong = Mockito.mock(RAtomicLong.class);
            Mockito.when(atomicLong.incrementAndGet()).thenAnswer(invocation -> getAtomicCounter(key).incrementAndGet());
            Mockito.when(atomicLong.expire(any(Duration.class))).thenAnswer(invocation -> true);
            Mockito.when(atomicLong.delete()).thenAnswer(invocation -> {
                boolean existed = atomicCounters.containsKey(key);
                atomicCounters.remove(key);
                return existed;
            });
            return atomicLong;
        }

        private void putValue(String key, Object value, long ttl) {
            bucketValues.put(key, value);
            ttlMillis.put(key, ttl);
        }

        Object getValue(String key) {
            return bucketValues.get(key);
        }

        private boolean exists(String key) {
            return bucketValues.containsKey(key) || atomicCounters.containsKey(key);
        }

        private AtomicLong getAtomicCounter(String key) {
            return atomicCounters.computeIfAbsent(key, ignored -> new AtomicLong());
        }
    }
}
