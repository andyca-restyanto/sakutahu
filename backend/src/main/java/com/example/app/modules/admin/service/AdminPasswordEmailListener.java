// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminPasswordEmailListener.java
package com.example.app.modules.admin.service;

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
public class AdminPasswordEmailListener {

    /** Admin tidak punya zona waktu sendiri; waktu ditampilkan dalam WIB. */
    private static final ZoneId ZONE = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy, HH.mm", Locale.forLanguageTag("id-ID"));

    private final MailService mailService;
    private final AppProperties props;

    public AdminPasswordEmailListener(MailService mailService, AppProperties props) {
        this.mailService = mailService;
        this.props = props;
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onResetRequested(AdminPasswordEmailEvents.ResetRequested event) {
        String link = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/admin/reset-password")
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();
        mailService.sendAdminPasswordReset(event.email(), event.fullName(), link, event.validFor(),
                event.sentByOwnerName());
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPasswordReset(AdminPasswordEmailEvents.PasswordReset event) {
        String when = TIME_FORMAT.format(event.changedAt().atZone(ZONE)) + " WIB";
        String forgotLink = UriComponentsBuilder.fromUriString(props.frontendUrl())
                .path("/admin/forgot-password").build().toUriString();
        mailService.sendAdminPasswordChanged(event.email(), event.fullName(), when, forgotLink);
    }
}
