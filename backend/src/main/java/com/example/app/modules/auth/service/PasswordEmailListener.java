// filepath: /backend/src/main/java/com/example/app/modules/auth/service/PasswordEmailListener.java
package com.example.app.modules.auth.service;

import com.example.app.shared.config.AppProperties;
import com.example.app.shared.config.AsyncConfig;
import com.example.app.shared.mail.MailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class PasswordEmailListener {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH.mm", Locale.forLanguageTag("id-ID"));

    private final MailService mailService;
    private final AppProperties props;

    public PasswordEmailListener(MailService mailService, AppProperties props) {
        this.mailService = mailService;
        this.props = props;
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onResetRequested(PasswordResetRequestedEvent event) {
        // Link ke halaman frontend; token ditukar lewat POST, tidak pernah dicatat di log server
        String link = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/reset-password")
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();
        mailService.sendPasswordReset(event.email(), event.fullName(), link, event.validFor());
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordChanged(PasswordChangedEvent event) {
        ZoneId zone;
        try {
            zone = ZoneId.of(event.timezone());
        } catch (Exception e) {
            zone = ZoneId.of("Asia/Jakarta");
        }
        String when = TIME_FORMAT.format(event.changedAt().atZone(zone)) + " (" + zone.getId() + ")";
        String forgotLink = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/forgot-password").build().toUriString();
        mailService.sendPasswordChanged(event.email(), event.fullName(), when, event.viaReset(), forgotLink);
    }
}
