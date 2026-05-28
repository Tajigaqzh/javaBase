package com.hp.javabase.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.mapper.UserLoginLogMapper;
import com.hp.javabase.mapper.UserMapper;
import com.hp.javabase.mapper.UserSessionDeviceMapper;
import com.hp.javabase.model.dto.ChangePasswordDTO;
import com.hp.javabase.model.dto.DeviceAwareDTO;
import com.hp.javabase.model.dto.ForgotPasswordResetDTO;
import com.hp.javabase.model.dto.PasswordLoginDTO;
import com.hp.javabase.model.dto.PhoneCodeLoginDTO;
import com.hp.javabase.model.dto.PhoneRegisterDTO;
import com.hp.javabase.model.dto.SetPasswordDTO;
import com.hp.javabase.model.entity.User;
import com.hp.javabase.model.entity.UserLoginLog;
import com.hp.javabase.model.entity.UserSessionDevice;
import com.hp.javabase.model.enums.RegisterSourceEnum;
import com.hp.javabase.model.enums.UserLoginStatusEnum;
import com.hp.javabase.model.enums.UserSessionStatusEnum;
import com.hp.javabase.model.enums.UserStatusEnum;
import com.hp.javabase.model.enums.VerificationSceneEnum;
import com.hp.javabase.model.vo.CurrentUserVO;
import com.hp.javabase.model.vo.LoginUserVO;
import com.hp.javabase.model.vo.UserSessionDeviceVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 前台认证服务，负责注册、登录、密码管理、设备会话管理和登录审计落库。
 *
 * <p>该服务统一编排前台一期认证规则：
 * 1. 手机号是主身份标识；
 * 2. 不同终端可共存，同端互斥；
 * 3. 设备信息、登录日志和设备会话必须同步记录；
 * 4. 改密、忘记密码、强制下线会立即影响现有会话。
 */
@Service
public class AuthService {

    private static final int LOGIN_FAIL_LOCK_LIMIT = 3;

    private final UserMapper userMapper;
    private final UserLoginLogMapper userLoginLogMapper;
    private final UserSessionDeviceMapper userSessionDeviceMapper;
    private final VerificationCodeUtils verificationCodeUtils;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserMapper userMapper,
            UserLoginLogMapper userLoginLogMapper,
            UserSessionDeviceMapper userSessionDeviceMapper,
            VerificationCodeUtils VerificationCodeUtils,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userLoginLogMapper = userLoginLogMapper;
        this.userSessionDeviceMapper = userSessionDeviceMapper;
        this.verificationCodeUtils = VerificationCodeUtils;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 手机号注册并立即建立前台登录态。
     *
     * @param request 注册请求
     * @param ip 登录来源 IP
     * @param userAgent 用户代理
     * @return 登录结果
     */
    @Transactional
    public LoginUserVO registerByPhone(PhoneRegisterDTO request, String ip, String userAgent) {
        verificationCodeUtils.verifyPhoneCode(request.getPhone(), VerificationSceneEnum.REGISTER, request.getSmsCode());
        if (userMapper.selectByPhone(request.getPhone()) != null) {
            throw new BusinessException("phone already registered");
        }
        validatePasswordStrength(request.getPassword(), request.getPhone(), request.getNickname());

        User user = new User();
        user.setUserNo(generateUserNo());
        user.setPhone(request.getPhone());
        user.setNickname(resolveNickname(request.getPhone(), request.getNickname()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        user.setRegisterSource(RegisterSourceEnum.PHONE.getCode());
        user.setIsPhoneVerified(1);
        user.setIsEmailVerified(0);
        user.setLoginFailCount(0);
        user.setIsDelete(0);
        userMapper.insert(user);

        return createLoginSession(user, request, ip, userAgent);
    }

    /**
     * 按手机号和密码执行登录，并在成功后建立设备会话与登录审计记录。
     *
     * @param request 密码登录请求
     * @param ip 登录来源 IP
     * @param userAgent 用户代理
     * @return 登录结果
     */
    @Transactional
    public LoginUserVO loginByPassword(PasswordLoginDTO request, String ip, String userAgent) {
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            recordLoginFailure(null, request.getPhone(), request, ip, userAgent, "user does not exist");
            throw new BusinessException("user does not exist");
        }
        validateLoginAllowed(user);
        if (!StringUtils.hasText(user.getPasswordHash())) {
            recordLoginFailure(user, user.getPhone(), request, ip, userAgent, "password not set");
            throw new BusinessException("password not set");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            increaseLoginFailCount(user);
            recordLoginFailure(user, user.getPhone(), request, ip, userAgent, "password is incorrect");
            throw new BusinessException("phone or password is incorrect");
        }
        clearLoginFailState(user.getId());
        return createLoginSession(user, request, ip, userAgent);
    }

    /**
     * 按手机号验证码执行登录。
     *
     * @param request 验证码登录请求
     * @param ip 登录来源 IP
     * @param userAgent 用户代理
     * @return 登录结果
     */
    @Transactional
    public LoginUserVO loginByPhoneCode(PhoneCodeLoginDTO request, String ip, String userAgent) {
        verificationCodeUtils.verifyPhoneCode(request.getPhone(), VerificationSceneEnum.LOGIN, request.getSmsCode());
        User user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            recordLoginFailure(null, request.getPhone(), request, ip, userAgent, "user does not exist");
            throw new BusinessException("user does not exist");
        }
        validateLoginAllowed(user);
        clearLoginFailState(user.getId());
        return createLoginSession(user, request, ip, userAgent);
    }

    /**
     * 首次设密或待补全补设密码。
     *
     * @param request 设密请求
     */
    @Transactional
    public void setPassword(SetPasswordDTO request) {
        User user = findUserByPhoneOrEmail(request.getPhone(), request.getEmail());
        if (StringUtils.hasText(request.getPhone())) {
            verificationCodeUtils.verifyPhoneCode(request.getPhone(), VerificationSceneEnum.SET_PASSWORD, request.getVerificationCode());
        } else {
            verificationCodeUtils.verifyEmailCode(request.getEmail(), VerificationSceneEnum.SET_PASSWORD, request.getVerificationCode());
        }
        if (StringUtils.hasText(user.getPasswordHash())) {
            throw new BusinessException("password already set");
        }
        validatePasswordStrength(request.getNewPassword(), user.getPhone(), user.getNickname());
        userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .set(User::getPasswordHash, passwordEncoder.encode(request.getNewPassword()))
                        .set(User::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 忘记密码后重置密码，并让该账号所有设备重新登录。
     *
     * @param request 忘记密码重置请求
     */
    @Transactional
    public void forgotResetPassword(ForgotPasswordResetDTO request) {
        User user = findUserByPhoneOrEmail(request.getPhone(), request.getEmail());
        if (StringUtils.hasText(request.getPhone())) {
            verificationCodeUtils.verifyPhoneCode(request.getPhone(), VerificationSceneEnum.FORGOT_PASSWORD, request.getVerificationCode());
        } else {
            verificationCodeUtils.verifyEmailCode(request.getEmail(), VerificationSceneEnum.FORGOT_PASSWORD, request.getVerificationCode());
        }
        validatePasswordStrength(request.getNewPassword(), user.getPhone(), user.getNickname());
        userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .set(User::getPasswordHash, passwordEncoder.encode(request.getNewPassword()))
                        .set(User::getLoginFailCount, 0)
                        .set(User::getLoginLockExpireTime, null)
                        .set(User::getUpdateTime, LocalDateTime.now()));
        logoutAllUserSessions(user.getId(), "PASSWORD_RESET");
    }

    /**
     * 已登录用户修改密码，必须校验旧密码，成功后全端强制重新登录。
     *
     * @param request 修改密码请求
     */
    @Transactional
    public void changePassword(ChangePasswordDTO request) {
        User user = requireCurrentUser();
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new BusinessException("password not set");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("old password is incorrect");
        }
        validatePasswordStrength(request.getNewPassword(), user.getPhone(), user.getNickname());
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException("new password must be different from old password");
        }
        userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .set(User::getPasswordHash, passwordEncoder.encode(request.getNewPassword()))
                        .set(User::getUpdateTime, LocalDateTime.now()));
        logoutAllUserSessions(user.getId(), "PASSWORD_CHANGED");
    }

    /**
     * 退出当前设备登录态，并回写设备会话和登录日志退出时间。
     */
    @Transactional
    public void logout() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return;
        }
        String tokenValue = StpUtil.getTokenValue();
        String tokenHash = hashToken(tokenValue);
        markSessionLogout(tokenHash, "LOGOUT");
        StpUtil.logout();
    }

    /**
     * 退出当前账号全部设备，可选择保留当前设备。
     *
     * @param keepCurrent 是否保留当前设备
     */
    @Transactional
    public void logoutAll(boolean keepCurrent) {
        User currentUser = requireCurrentUser();
        String currentTokenValue = StpUtil.getTokenValue();
        List<UserSessionDevice> onlineSessions = listOnlineSessions(currentUser.getId());
        for (UserSessionDevice session : onlineSessions) {
            if (keepCurrent && Objects.equals(currentTokenValue, session.getTokenValue())) {
                continue;
            }
            StpUtil.logoutByTokenValue(session.getTokenValue());
            markSessionByToken(session.getTokenHash(), UserSessionStatusEnum.LOGOUT, "LOGOUT_ALL");
        }
        if (!keepCurrent) {
            StpUtil.logout(currentUser.getId());
        }
    }

    /**
     * 获取当前账号在线设备列表，并标记当前设备。
     *
     * @return 在线设备视图列表
     */
    public List<UserSessionDeviceVO> listMySessions() {
        User currentUser = requireCurrentUser();
        String currentTokenHash = hashToken(StpUtil.getTokenValue());
        return listOnlineSessions(currentUser.getId()).stream()
                .map(session -> UserSessionDeviceVO.builder()
                        .sessionId(session.getId())
                        .terminalType(session.getTerminalType())
                        .deviceId(session.getDeviceId())
                        .deviceName(session.getDeviceName())
                        .fingerprint(session.getFingerprint())
                        .ip(session.getIp())
                        .userAgent(session.getUserAgent())
                        .sessionStatus(session.getSessionStatus())
                        .loginTime(session.getLoginTime())
                        .lastActiveTime(session.getLastActiveTime())
                        .currentDevice(Objects.equals(currentTokenHash, session.getTokenHash()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 用户侧按设备会话记录踢指定设备下线，但不允许踢当前设备。
     *
     * @param sessionId 设备会话记录 ID
     */
    @Transactional
    public void kickoutSession(Long sessionId) {
        User currentUser = requireCurrentUser();
        UserSessionDevice session = userSessionDeviceMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), currentUser.getId())) {
            throw new BusinessException("session does not exist");
        }
        if (!Objects.equals(session.getSessionStatus(), UserSessionStatusEnum.ONLINE.getCode())) {
            throw new BusinessException("session is not online");
        }
        if (Objects.equals(session.getTokenHash(), hashToken(StpUtil.getTokenValue()))) {
            throw new BusinessException("current device cannot be kicked out");
        }
        StpUtil.logoutByTokenValue(session.getTokenValue());
        markSessionByToken(session.getTokenHash(), UserSessionStatusEnum.KICKOUT, "MANUAL_KICKOUT");
    }

    public CurrentUserVO getCurrentUserProfile() {
        User user = requireCurrentUser();
        return CurrentUserVO.builder()
                .userId(user.getId())
                .userNo(user.getUserNo())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .build();
    }

    public User findCurrentUser() {
        return findByLoginId(StpUtil.getLoginIdDefaultNull());
    }

    public User findByLoginId(Object loginId) {
        if (loginId == null) {
            return null;
        }
        return userMapper.selectById(Long.valueOf(String.valueOf(loginId)));
    }

    private LoginUserVO createLoginSession(User user, DeviceAwareDTO request, String ip, String userAgent) {
        replaceSameTerminalSessions(user.getId(), request.getTerminalType());

        StpUtil.login(
                user.getId(),
                SaLoginParameter.create()
                        .setDeviceType(normalizeTerminalType(request.getTerminalType()))
                        .setDeviceId(request.getDeviceId())
                        .setIsConcurrent(Boolean.FALSE)
                        .setIsShare(Boolean.FALSE));
        String tokenValue = StpUtil.getTokenValue();
        String tokenHash = hashToken(tokenValue);
        LocalDateTime now = LocalDateTime.now();

        userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, user.getId())
                        .set(User::getLoginFailCount, 0)
                        .set(User::getLoginLockExpireTime, null)
                        .set(User::getLastLoginTime, now)
                        .set(User::getLastLoginIp, ip)
                        .set(User::getUpdateTime, now));

        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user.getId());
        loginLog.setPhone(user.getPhone());
        loginLog.setTerminalType(normalizeTerminalType(request.getTerminalType()));
        loginLog.setDeviceId(request.getDeviceId());
        loginLog.setFingerprint(request.getFingerprint());
        loginLog.setDeviceName(request.getDeviceName());
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginStatus(UserLoginStatusEnum.SUCCESS.getCode());
        loginLog.setTokenHash(tokenHash);
        loginLog.setLoginTime(now);
        userLoginLogMapper.insert(loginLog);

        UserSessionDevice sessionDevice = new UserSessionDevice();
        sessionDevice.setUserId(user.getId());
        sessionDevice.setTerminalType(normalizeTerminalType(request.getTerminalType()));
        sessionDevice.setDeviceId(request.getDeviceId());
        sessionDevice.setFingerprint(request.getFingerprint());
        sessionDevice.setDeviceName(request.getDeviceName());
        sessionDevice.setTokenValue(tokenValue);
        sessionDevice.setTokenHash(tokenHash);
        sessionDevice.setIp(ip);
        sessionDevice.setUserAgent(userAgent);
        sessionDevice.setSessionStatus(UserSessionStatusEnum.ONLINE.getCode());
        sessionDevice.setLoginTime(now);
        sessionDevice.setLastActiveTime(now);
        userSessionDeviceMapper.insert(sessionDevice);

        return LoginUserVO.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .token(tokenValue)
                .terminalType(normalizeTerminalType(request.getTerminalType()))
                .deviceId(request.getDeviceId())
                .status(user.getStatus())
                .build();
    }

    private void validateLoginAllowed(User user) {
        if (Objects.equals(user.getIsDelete(), 1) || Objects.equals(user.getStatus(), UserStatusEnum.CANCELLED.getCode())) {
            throw new BusinessException("account can be recovered only through recover flow");
        }
        if (Objects.equals(user.getStatus(), UserStatusEnum.DISABLED.getCode())) {
            throw new BusinessException("account is disabled");
        }
        if (Objects.equals(user.getStatus(), UserStatusEnum.FROZEN.getCode())) {
            throw new BusinessException("account is frozen");
        }
        if (user.getLoginLockExpireTime() != null && user.getLoginLockExpireTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("account is temporarily locked");
        }
    }

    private void validatePasswordStrength(String password, String phone, String nickname) {
        if (!StringUtils.hasText(password) || password.length() < 10 || password.length() > 64) {
            throw new BusinessException("password length must be between 10 and 64");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)
                || !password.chars().anyMatch(Character::isLowerCase)
                || !password.chars().anyMatch(Character::isDigit)
                || password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new BusinessException("password must contain upper, lower, digit and special character");
        }
        if (StringUtils.hasText(nickname) && password.toLowerCase(Locale.ROOT).contains(nickname.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("password must not contain nickname");
        }
        if (StringUtils.hasText(phone) && phone.length() >= 6) {
            String lastFour = phone.substring(phone.length() - 4);
            String lastSix = phone.substring(phone.length() - 6);
            if (password.contains(lastFour) || password.contains(lastSix)) {
                throw new BusinessException("password must not contain phone tail digits");
            }
        }
    }

    private void increaseLoginFailCount(User user) {
        int nextFailCount = user.getLoginFailCount() == null ? 1 : user.getLoginFailCount() + 1;
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getLoginFailCount, nextFailCount)
                .set(User::getUpdateTime, LocalDateTime.now());
        if (nextFailCount >= LOGIN_FAIL_LOCK_LIMIT) {
            updateWrapper.set(User::getLoginLockExpireTime, LocalDateTime.now().plusHours(1));
        }
        userMapper.update(null, updateWrapper);
    }

    private void clearLoginFailState(Long userId) {
        userMapper.update(
                null,
                new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getLoginFailCount, 0)
                        .set(User::getLoginLockExpireTime, null)
                        .set(User::getUpdateTime, LocalDateTime.now()));
    }

    private void replaceSameTerminalSessions(Long userId, String terminalType) {
        List<UserSessionDevice> currentTerminalSessions = userSessionDeviceMapper.selectList(
                new LambdaQueryWrapper<UserSessionDevice>()
                        .eq(UserSessionDevice::getUserId, userId)
                        .eq(UserSessionDevice::getTerminalType, normalizeTerminalType(terminalType))
                        .eq(UserSessionDevice::getSessionStatus, UserSessionStatusEnum.ONLINE.getCode()));
        for (UserSessionDevice session : currentTerminalSessions) {
            StpUtil.logoutByTokenValue(session.getTokenValue());
            markSessionByToken(session.getTokenHash(), UserSessionStatusEnum.KICKOUT, "SAME_TERMINAL_REPLACED");
        }
    }

    private void recordLoginFailure(
            User user,
            String phone,
            DeviceAwareDTO request,
            String ip,
            String userAgent,
            String failReason) {
        UserLoginLog loginLog = new UserLoginLog();
        loginLog.setUserId(user == null ? null : user.getId());
        loginLog.setPhone(phone);
        loginLog.setTerminalType(normalizeTerminalType(request.getTerminalType()));
        loginLog.setDeviceId(request.getDeviceId());
        loginLog.setFingerprint(request.getFingerprint());
        loginLog.setDeviceName(request.getDeviceName());
        loginLog.setIp(ip);
        loginLog.setUserAgent(userAgent);
        loginLog.setLoginStatus(UserLoginStatusEnum.FAIL.getCode());
        loginLog.setFailReason(failReason);
        loginLog.setLoginTime(LocalDateTime.now());
        userLoginLogMapper.insert(loginLog);
    }

    private void markSessionLogout(String tokenHash, String reason) {
        markSessionByToken(tokenHash, UserSessionStatusEnum.LOGOUT, reason);
        LocalDateTime now = LocalDateTime.now();
        userLoginLogMapper.update(
                null,
                new LambdaUpdateWrapper<UserLoginLog>()
                        .eq(UserLoginLog::getTokenHash, tokenHash)
                        .isNull(UserLoginLog::getLogoutTime)
                        .set(UserLoginLog::getLogoutTime, now)
                        .set(UserLoginLog::getUpdateTime, now));
    }

    private void markSessionByToken(String tokenHash, UserSessionStatusEnum statusEnum, String reason) {
        LocalDateTime now = LocalDateTime.now();
        userSessionDeviceMapper.update(
                null,
                new LambdaUpdateWrapper<UserSessionDevice>()
                        .eq(UserSessionDevice::getTokenHash, tokenHash)
                        .eq(UserSessionDevice::getSessionStatus, UserSessionStatusEnum.ONLINE.getCode())
                        .set(UserSessionDevice::getSessionStatus, statusEnum.getCode())
                        .set(UserSessionDevice::getLastActiveTime, now)
                        .set(UserSessionDevice::getLogoutTime, statusEnum == UserSessionStatusEnum.LOGOUT ? now : null)
                        .set(UserSessionDevice::getKickoutTime, statusEnum == UserSessionStatusEnum.KICKOUT ? now : null)
                        .set(UserSessionDevice::getKickoutReason, reason)
                        .set(UserSessionDevice::getUpdateTime, now));
        userLoginLogMapper.update(
                null,
                new LambdaUpdateWrapper<UserLoginLog>()
                        .eq(UserLoginLog::getTokenHash, tokenHash)
                        .set(UserLoginLog::getLogoutTime, statusEnum == UserSessionStatusEnum.LOGOUT ? now : null)
                        .set(UserLoginLog::getKickoutTime, statusEnum == UserSessionStatusEnum.KICKOUT ? now : null)
                        .set(UserLoginLog::getKickoutReason, reason)
                        .set(UserLoginLog::getUpdateTime, now));
    }

    private void logoutAllUserSessions(Long userId, String reason) {
        List<UserSessionDevice> onlineSessions = listOnlineSessions(userId);
        StpUtil.logout(userId);
        for (UserSessionDevice session : onlineSessions) {
            markSessionByToken(session.getTokenHash(), UserSessionStatusEnum.KICKOUT, reason);
        }
    }

    private List<UserSessionDevice> listOnlineSessions(Long userId) {
        return userSessionDeviceMapper.selectList(
                new LambdaQueryWrapper<UserSessionDevice>()
                        .eq(UserSessionDevice::getUserId, userId)
                        .eq(UserSessionDevice::getSessionStatus, UserSessionStatusEnum.ONLINE.getCode())
                        .orderByDesc(UserSessionDevice::getLoginTime));
    }

    private User requireCurrentUser() {
        User user = findCurrentUser();
        if (user == null) {
            throw new BusinessException("current user does not exist");
        }
        return user;
    }

    private User findUserByPhoneOrEmail(String phone, String email) {
        boolean hasPhone = StringUtils.hasText(phone);
        boolean hasEmail = StringUtils.hasText(email);
        if (hasPhone == hasEmail) {
            throw new BusinessException("exactly one of phone or email must be provided");
        }
        User user = hasPhone ? userMapper.selectByPhone(phone) : userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException("user does not exist");
        }
        return user;
    }

    private String resolveNickname(String phone, String nickname) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        return "用户" + phone.substring(phone.length() - 4);
    }

    private String generateUserNo() {
        return "U" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
    }

    private String normalizeTerminalType(String terminalType) {
        if (!StringUtils.hasText(terminalType)) {
            return "UNKNOWN";
        }
        return terminalType.trim().toUpperCase(Locale.ROOT);
    }

    private String hashToken(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("sha-256 algorithm unavailable", exception);
        }
    }
}
