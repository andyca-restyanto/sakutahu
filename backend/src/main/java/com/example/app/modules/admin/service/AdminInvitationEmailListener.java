// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminInvitationEmailListener.java
package com.example.app.modules.admin.service;

import com.example.app.shared.config.AdminProperties;
import com.example.app.shared.config.AppProperties;
import com.example.app.shared.config.AsyncConfig;
import com.example.app.shared.mail.MailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AdminInvitationEmailListener {

    private final MailService mailService;
    private final AppProperties appProps;
    private final AdminProperties adminProps;

    public AdminInvitationEmailListener(MailService mailService, AppProperties appProps, AdminProperties adminProps) {
        this.mailService = mailService;
        this.appProps = appProps;
        this.adminProps = adminProps;
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvitation(AdminInvitationEmailEvent event) {
        // Link ke halaman frontend admin; token di query string, ditukar lewat POST (tidak tersimpan di log server)
        String link = UriComponentsBuilder.fromUriString(appProps.frontendUrl())
                .path("/admin/accept-invite")
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();
        mailService.sendAdminInvitation(event.email(), event.fullName(), event.inviterName(), link,
                adminProps.invitationTtl());
    }
}
