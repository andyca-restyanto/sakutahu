// filepath: /backend/src/main/java/com/example/app/modules/admin/controller/AdminInvitationController.java
package com.example.app.modules.admin.controller;

import com.example.app.modules.admin.dto.AcceptInvitationRequest;
import com.example.app.modules.admin.dto.AdminAuthResponse;
import com.example.app.modules.admin.dto.InvitationInfoResponse;
import com.example.app.modules.admin.dto.InvitationTokenRequest;
import com.example.app.modules.admin.service.AdminAuthResult;
import com.example.app.modules.admin.service.AdminInvitationService;
import com.example.app.shared.util.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint publik untuk penerima undangan (belum punya akun admin). */
@RestController
@RequestMapping("/api/v1/admin/invitations")
public class AdminInvitationController {

    private final AdminInvitationService invitationService;
    private final AdminRefreshCookieFactory cookies;

    public AdminInvitationController(AdminInvitationService invitationService, AdminRefreshCookieFactory cookies) {
        this.invitationService = invitationService;
        this.cookies = cookies;
    }

    @PostMapping("/inspect")
    public InvitationInfoResponse inspect(@Valid @RequestBody InvitationTokenRequest request, HttpServletRequest http) {
        return invitationService.inspect(request.token(), ClientContext.from(http));
    }

    @PostMapping("/accept")
    public ResponseEntity<AdminAuthResponse> accept(@Valid @RequestBody AcceptInvitationRequest request,
                                                    HttpServletRequest http) {
        AdminAuthResult result = invitationService.accept(request, ClientContext.from(http));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()).toString())
                .body(result.response());
    }
}
