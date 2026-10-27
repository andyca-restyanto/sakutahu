// filepath: /backend/src/main/java/com/example/app/modules/telegram/controller/TelegramController.java
package com.example.app.modules.telegram.controller;

import com.example.app.modules.telegram.dto.TelegramLinkResponse;
import com.example.app.modules.telegram.dto.TelegramStatusResponse;
import com.example.app.modules.telegram.dto.TelegramUpdate;
import com.example.app.modules.telegram.dto.TelegramWebhookReply;
import com.example.app.modules.telegram.service.TelegramAccountService;
import com.example.app.modules.telegram.service.TelegramLinkService;
import com.example.app.modules.telegram.service.TelegramUpdateRouter;
import com.example.app.shared.config.AppProperties;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.security.UserPrincipal;
import com.example.app.shared.util.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/telegram")
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramLinkService linkService;
    private final TelegramUpdateRouter router;
    private final TelegramAccountService accountService;
    private final RateLimiter rateLimiter;
    private final AppProperties props;

    public TelegramController(TelegramLinkService linkService,
                              TelegramUpdateRouter router,
                              TelegramAccountService accountService,
                              RateLimiter rateLimiter,
                              AppProperties props) {
        this.linkService = linkService;
        this.router = router;
        this.accountService = accountService;
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    @PostMapping("/link-token")
    public TelegramLinkResponse createLinkToken(@AuthenticationPrincipal UserPrincipal principal) {
        rateLimiter.consume("tg-link:" + principal.id(), 10, Duration.ofMinutes(10));
        return linkService.createLinkToken(principal.id());
    }

    @GetMapping("/status")
    public TelegramStatusResponse status(@AuthenticationPrincipal UserPrincipal principal) {
        return accountService.status(principal.id());
    }

    @DeleteMapping("/link")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal UserPrincipal principal) {
        accountService.unlink(principal.id());
        return ResponseEntity.noContent().build();
    }

    /** Endpoint webhook untuk Telegram (setWebhook dengan secret_token). */
    @PostMapping("/webhook")
    public ResponseEntity<TelegramWebhookReply> webhook(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody TelegramUpdate update) {
        String expected = props.telegram().webhookSecret();
        if (expected == null || expected.isBlank() || !TokenUtils.constantTimeEquals(expected, secret)) {
            log.warn("Webhook Telegram ditolak: secret tidak cocok atau belum dikonfigurasi");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // Selalu 200 agar Telegram tidak mengulang update yang sama
        return router.route(update)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok().build());
    }
}
