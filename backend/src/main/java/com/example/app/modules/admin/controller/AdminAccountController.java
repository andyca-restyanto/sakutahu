// filepath: /backend/src/main/java/com/example/app/modules/admin/controller/AdminAccountController.java
package com.example.app.modules.admin.controller;

import com.example.app.modules.admin.dto.AdminInvitationResponse;
import com.example.app.modules.admin.dto.AdminResponse;
import com.example.app.modules.admin.dto.InviteAdminRequest;
import com.example.app.modules.admin.dto.UpdateAdminStatusRequest;
import com.example.app.modules.admin.service.AdminAccountService;
import com.example.app.modules.admin.service.AdminInvitationService;
import com.example.app.shared.security.AdminPrincipal;
import com.example.app.shared.util.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Menu "Admin". Seluruh path /api/v1/admin/admins/** hanya untuk OWNER (SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/admins")
public class AdminAccountController {

    private final AdminAccountService accountService;
    private final AdminInvitationService invitationService;

    public AdminAccountController(AdminAccountService accountService, AdminInvitationService invitationService) {
        this.accountService = accountService;
        this.invitationService = invitationService;
    }

    @GetMapping
    public List<AdminResponse> list() {
        return accountService.list();
    }

    @PatchMapping("/{id}/status")
    public AdminResponse changeStatus(@AuthenticationPrincipal AdminPrincipal owner, @PathVariable UUID id,
                                      @Valid @RequestBody UpdateAdminStatusRequest request, HttpServletRequest http) {
        return accountService.changeStatus(owner.id(), id, request.status(), ClientContext.from(http));
    }

    @GetMapping("/invitations")
    public List<AdminInvitationResponse> invitations() {
        return invitationService.listPending();
    }

    @PostMapping("/invitations")
    public ResponseEntity<AdminInvitationResponse> invite(@AuthenticationPrincipal AdminPrincipal owner,
                                                          @Valid @RequestBody InviteAdminRequest request,
                                                          HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.invite(owner.id(), request, ClientContext.from(http)));
    }

    @PostMapping("/invitations/{id}/resend")
    public AdminInvitationResponse resend(@AuthenticationPrincipal AdminPrincipal owner, @PathVariable UUID id,
                                          HttpServletRequest http) {
        return invitationService.resend(owner.id(), id, ClientContext.from(http));
    }

    @DeleteMapping("/invitations/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal AdminPrincipal owner, @PathVariable UUID id,
                                       HttpServletRequest http) {
        invitationService.revoke(owner.id(), id, ClientContext.from(http));
        return ResponseEntity.noContent().build();
    }
}
