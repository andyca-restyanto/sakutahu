// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminInvitationService.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AcceptInvitationRequest;
import com.example.app.modules.admin.dto.AdminInvitationResponse;
import com.example.app.modules.admin.dto.InvitationInfoResponse;
import com.example.app.modules.admin.dto.InviteAdminRequest;
import com.example.app.modules.admin.entity.AdminInvitation;
import com.example.app.modules.admin.entity.AdminRole;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminInvitationRepository;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.shared.config.AdminProperties;
import com.example.app.shared.exception.ApiException;
import com.example.app.shared.exception.ErrorCode;
import com.example.app.shared.ratelimit.RateLimiter;
import com.example.app.shared.util.ClientContext;
import com.example.app.shared.util.TokenUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Undangan admin lewat email: OWNER mengundang → penerima membuat password sendiri. */
@Service
public class AdminInvitationService {

    private final AdminInvitationRepository invitationRepository;
    private final AdminUserRepository adminRepository;
    private final AdminSessionService sessionService;
    private final AdminAuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final RateLimiter rateLimiter;
    private final AdminProperties props;
    private final Clock clock;

    public AdminInvitationService(AdminInvitationRepository invitationRepository, AdminUserRepository adminRepository,
                                  AdminSessionService sessionService, AdminAuditService auditService,
                                  PasswordEncoder passwordEncoder, ApplicationEventPublisher events,
                                  RateLimiter rateLimiter, AdminProperties props, Clock clock) {
        this.invitationRepository = invitationRepository;
        this.adminRepository = adminRepository;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.rateLimiter = rateLimiter;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminInvitationResponse> listPending() {
        List<AdminInvitation> pending = invitationRepository.findPending();
        Map<UUID, String> names = adminRepository.findAllById(
                        pending.stream().map(AdminInvitation::getInvitedBy).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(AdminUser::getId, AdminUser::getFullName, (a, b) -> a));
        Instant now = clock.instant();
        return pending.stream().map(i -> toResponse(i, names, now)).toList();
    }

    @Transactional
    public AdminInvitationResponse invite(UUID ownerId, InviteAdminRequest request, ClientContext ctx) {
        rateLimiter.consume("admin-invite:" + ownerId, 20, Duration.ofHours(1));
        String email = AdminMapper.normalizeEmail(request.email());
        if (adminRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.ADMIN_EMAIL_EXISTS);
        }
        Instant now = clock.instant();
        // Undangan lama untuk email yang sama tidak berlaku lagi
        invitationRepository.findPendingByEmail(email).ifPresent(old -> {
            old.revoke(now);
            invitationRepository.saveAndFlush(old);
        });

        String raw = TokenUtils.randomToken();
        AdminInvitation invitation = invitationRepository.save(new AdminInvitation(email, request.fullName().trim(),
                TokenUtils.sha256Hex(raw), ownerId, now.plus(props.invitationTtl())));
        AdminUser owner = loadAdmin(ownerId);
        events.publishEvent(new AdminInvitationEmailEvent(email, invitation.getFullName(), owner.getFullName(), raw));
        auditService.record(ownerId, "ADMIN_INVITED", AdminAuditService.TARGET_INVITATION, invitation.getId(),
                AdminAuditService.detail("email", email, "fullName", invitation.getFullName()), ctx.ip());
        return toResponse(invitation, Map.of(ownerId, owner.getFullName()), now);
    }

    /** Kirim ulang: token baru, masa berlaku diperpanjang, link lama mati. */
    @Transactional
    public AdminInvitationResponse resend(UUID ownerId, UUID invitationId, ClientContext ctx) {
        rateLimiter.consume("admin-invite-resend:" + invitationId, 3, Duration.ofHours(1));
        AdminInvitation invitation = loadPending(invitationId);
        Instant now = clock.instant();
        String raw = TokenUtils.randomToken();
        invitation.renew(TokenUtils.sha256Hex(raw), now.plus(props.invitationTtl()));
        AdminUser owner = loadAdmin(ownerId);
        events.publishEvent(new AdminInvitationEmailEvent(invitation.getEmail(), invitation.getFullName(),
                owner.getFullName(), raw));
        auditService.record(ownerId, "ADMIN_INVITATION_RESENT", AdminAuditService.TARGET_INVITATION, invitationId,
                AdminAuditService.detail("email", invitation.getEmail()), ctx.ip());
        return toResponse(invitation, Map.of(ownerId, owner.getFullName()), now);
    }

    @Transactional
    public void revoke(UUID ownerId, UUID invitationId, ClientContext ctx) {
        AdminInvitation invitation = loadPending(invitationId);
        invitation.revoke(clock.instant());
        auditService.record(ownerId, "ADMIN_INVITATION_REVOKED", AdminAuditService.TARGET_INVITATION, invitationId,
                AdminAuditService.detail("email", invitation.getEmail()), ctx.ip());
    }

    /** Dipanggil halaman "terima undangan" untuk menampilkan email & nama sebelum membuat password. */
    @Transactional(readOnly = true)
    public InvitationInfoResponse inspect(String rawToken, ClientContext ctx) {
        rateLimiter.consume("admin-invite-inspect:" + ctx.ip(), 30, Duration.ofMinutes(10));
        AdminInvitation invitation = validToken(rawToken);
        return new InvitationInfoResponse(invitation.getEmail(), invitation.getFullName(), invitation.getExpiresAt());
    }

    @Transactional
    public AdminAuthResult accept(AcceptInvitationRequest request, ClientContext ctx) {
        rateLimiter.consume("admin-invite-accept:" + ctx.ip(), 10, Duration.ofMinutes(10));
        AdminInvitation invitation = validToken(request.token());
        if (adminRepository.existsByEmail(invitation.getEmail())) {
            throw new ApiException(ErrorCode.ADMIN_EMAIL_EXISTS);
        }
        Instant now = clock.instant();
        AdminUser admin = adminRepository.save(new AdminUser(invitation.getEmail(), invitation.getFullName(),
                passwordEncoder.encode(request.password()), AdminRole.ADMIN, false, invitation.getInvitedBy()));
        invitation.accept(now);
        admin.recordLogin(now);
        auditService.record(admin.getId(), "ADMIN_INVITATION_ACCEPTED", AdminAuditService.TARGET_ADMIN, admin.getId(),
                AdminAuditService.detail("email", admin.getEmail(), "invitationId", invitation.getId()), ctx.ip());
        return sessionService.issue(admin, ctx);
    }

    private AdminInvitation validToken(String rawToken) {
        AdminInvitation invitation = invitationRepository.findByTokenHash(TokenUtils.sha256Hex(rawToken.trim()))
                .filter(AdminInvitation::isPending)
                .orElseThrow(() -> new ApiException(ErrorCode.INVITATION_INVALID));
        if (invitation.isExpired(clock.instant())) {
            throw new ApiException(ErrorCode.INVITATION_EXPIRED);
        }
        return invitation;
    }

    private AdminInvitation loadPending(UUID id) {
        return invitationRepository.findById(id)
                .filter(AdminInvitation::isPending)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Undangan tidak ditemukan"));
    }

    private AdminUser loadAdmin(UUID id) {
        return adminRepository.findById(id).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }

    private static AdminInvitationResponse toResponse(AdminInvitation i, Map<UUID, String> names, Instant now) {
        return new AdminInvitationResponse(i.getId(), i.getEmail(), i.getFullName(),
                i.getInvitedBy() == null ? null : names.get(i.getInvitedBy()),
                i.getExpiresAt(), i.isExpired(now), i.getCreatedAt());
    }
}
