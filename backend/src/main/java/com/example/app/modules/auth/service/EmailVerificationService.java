// filepath: /backend/src/main/java/com/example/app/modules/auth/service/EmailVerificationService.java
package com.example.app.modules.auth.service;

import com.example.app.modules.auth.entity.EmailVerificationToken;
import com.example.app.modules.auth.entity.User;
import com.example.app.modules.auth.repository.EmailVerificationTokenRepository;
import com.example.app.modules.auth.repository.UserRepository;
import com.example.app.shared.config.AppProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.util.TokenUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final Clock clock;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserRepository userRepository,
                                    ApplicationEventPublisher events,
                                    AppProperties props,
                                    Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.props = props;
        this.clock = clock;
    }

    /** Membuat token baru (token lama otomatis tidak berlaku) lalu menjadwalkan email. */
    @Transactional
    public void createAndSend(User user) {
        Instant now = clock.instant();
        tokenRepository.invalidateActiveForUser(user.getId(), now);

        String raw = TokenUtils.randomToken();
        tokenRepository.save(new EmailVerificationToken(
                user.getId(), TokenUtils.sha256Hex(raw), now.plus(props.verification().tokenTtl())));

        events.publishEvent(new VerificationEmailRequestedEvent(user.getEmail(), user.getFullName(), raw));
    }

    /** Memverifikasi token dan menandai email user terverifikasi. */
    @Transactional
    public User verify(String rawToken) {
        Instant now = clock.instant();
        EmailVerificationToken token = tokenRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFICATION_TOKEN_INVALID));

        if (token.isUsed()) {
            throw new ApiException(ErrorCode.VERIFICATION_TOKEN_INVALID);
        }
        if (token.isExpired(now)) {
            throw new ApiException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.VERIFICATION_TOKEN_INVALID));

        token.markUsed(now);
        tokenRepository.save(token);
        tokenRepository.invalidateActiveForUser(user.getId(), now);

        user.markEmailVerified(now);
        return userRepository.save(user);
    }
}
