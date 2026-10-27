// filepath: /backend/src/main/java/com/example/app/modules/auth/service/VerificationEmailListener.java
package com.example.app.modules.auth.service;

import com.example.app.shared.config.AppProperties;
import com.example.app.shared.config.AsyncConfig;
import com.example.app.shared.mail.MailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class VerificationEmailListener {

    private final MailService mailService;
    private final AppProperties props;

    public VerificationEmailListener(MailService mailService, AppProperties props) {
        this.mailService = mailService;
        this.props = props;
    }

    /**
     * Dikirim setelah commit (token pasti sudah tersimpan) dan secara asinkron
     * agar request register tidak tertahan oleh SMTP.
     */
    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onVerificationEmailRequested(VerificationEmailRequestedEvent event) {
        // Link mengarah ke halaman frontend, bukan langsung ke API (aman dari pemindai link email)
        String link = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/verify-email")
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();
        mailService.sendVerificationEmail(event.email(), event.fullName(), link, props.verification().tokenTtl());
    }
}
