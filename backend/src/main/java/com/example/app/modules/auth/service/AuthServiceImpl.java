// filepath: /backend/src/main/java/com/example/app/modules/auth/service/AuthServiceImpl.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.dto.LoginRequest;
import com.example.app.modules.auth.dto.MessageResponse;
import com.example.app.modules.auth.dto.RegisterRequest;
import com.example.app.modules.auth.dto.RegisterResponse;
import com.example.app.modules.auth.dto.ResendVerificationRequest;
import com.example.app.modules.auth.dto.UserResponse;
import com.example.app.modules.auth.dto.VerifyEmailRequest;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.config.AppProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.util.ClientContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    static final String DEFAULT_CURRENCY = "IDR";
    static final String DEFAULT_TIMEZONE = "Asia/Jakarta";
    static final String MSG_REGISTERED = "Link verifikasi telah dikirim ke email kamu";
    static final String MSG_RESEND = "Jika email terdaftar dan belum terverifikasi, link baru telah dikirim";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final UserResponseMapper userResponseMapper;
    private final RateLimiter rateLimiter;
    private final AppProperties props;
    private final Clock clock;

    /** Hash dummy untuk menyamakan waktu respons saat email tidak ditemukan (anti user enumeration). */
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           EmailVerificationService emailVerificationService,
                           RefreshTokenService refreshTokenService,
                           UserResponseMapper userResponseMapper,
                           RateLimiter rateLimiter,
                           AppProperties props,
                           Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
        this.userResponseMapper = userResponseMapper;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("sakutahu-dummy-password-1");
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request, ClientContext ctx) {
        rateLimiter.consume("register:" + ctx.ip(), 5, Duration.ofMinutes(1));
        String email = normalizeEmail(request.email());

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.isEmailVerified()) {
                throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            // Belum terverifikasi: kirim ulang link, data akun TIDAK ditimpa
            // (mencegah pihak lain mengganti password akun yang belum diverifikasi).
            if (rateLimiter.tryConsume("resend:" + email, 1, Duration.ofMinutes(1))) {
                emailVerificationService.createAndSend(user);
            }
            return new RegisterResponse(email, true, MSG_REGISTERED);
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.currency() != null ? request.currency() : DEFAULT_CURRENCY,
                resolveTimezone(request.timezone()));
        userRepository.save(user);
        rateLimiter.tryConsume("resend:" + email, 1, Duration.ofMinutes(1));
        emailVerificationService.createAndSend(user);

        return new RegisterResponse(email, true, MSG_REGISTERED);
    }

    @Override
    @Transactional
    public AuthResult verifyEmail(VerifyEmailRequest request, ClientContext ctx) {
        rateLimiter.consume("verify:" + ctx.ip(), 20, Duration.ofMinutes(1));
        User user = emailVerificationService.verify(request.token().trim());
        return refreshTokenService.issue(user, ctx);
    }

    @Override
    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request, ClientContext ctx) {
        String email = normalizeEmail(request.email());
        // Dibatasi per email, berlaku sama untuk email terdaftar maupun tidak (tidak membocorkan keberadaan akun)
        rateLimiter.consume("resend:" + email, 1, Duration.ofMinutes(1));
        rateLimiter.consume("resend-hourly:" + email, 5, Duration.ofHours(1));
        rateLimiter.consume("resend-ip:" + ctx.ip(), 20, Duration.ofHours(1));

        userRepository.findByEmail(email)
                .filter(u -> !u.isEmailVerified())
                .ifPresent(emailVerificationService::createAndSend);

        return new MessageResponse(MSG_RESEND);
    }

    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResult login(LoginRequest request, ClientContext ctx) {
        String email = normalizeEmail(request.email());
        rateLimiter.consume("login:" + ctx.ip() + ":" + email, 5, Duration.ofMinutes(1));

        Instant now = clock.instant();
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();

        if (user.isLocked(now)) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.registerFailedLogin(props.security().maxFailedLogins(), props.security().lockDuration(), now);
            userRepository.save(user);
            throw new ApiException(user.isLocked(now) ? ErrorCode.ACCOUNT_LOCKED : ErrorCode.INVALID_CREDENTIALS);
        }

        user.resetFailedLogins();
        userRepository.save(user);

        // Dicek setelah password benar agar status verifikasi tidak bocor ke pihak yang tidak tahu password
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        return refreshTokenService.issue(user, ctx);
    }

    @Override
    public AuthResult refresh(String rawRefreshToken, ClientContext ctx) {
        rateLimiter.consume("refresh:" + ctx.ip(), 30, Duration.ofMinutes(1));
        return refreshTokenService.rotate(rawRefreshToken, ctx);
    }

    @Override
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return userResponseMapper.toResponse(user);
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TIMEZONE;
        }
        try {
            return ZoneId.of(timezone.trim()).getId();
        } catch (DateTimeException e) {
            return DEFAULT_TIMEZONE;
        }
    }
}
