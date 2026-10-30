// filepath: /backend/src/main/java/com/example/app/modules/admin/controller/AdminUserController.java
package com.example.app.modules.admin.controller;

import com.example.app.modules.admin.dto.AdminUserDetailResponse;
import com.example.app.modules.admin.dto.AdminUserStatusFilter;
import com.example.app.modules.admin.dto.AdminUserSummaryResponse;
import com.example.app.modules.admin.dto.BlockUserRequest;
import com.example.app.modules.admin.dto.ChangePlanRequest;
import com.example.app.modules.admin.service.AdminUserService;
import com.example.app.modules.subscription.entity.UserPlan;
import com.example.app.shared.security.AdminPrincipal;
import com.example.app.shared.util.ClientContext;
import com.example.app.shared.web.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Menu "User": daftar, detail, ubah plan, blokir/buka blokir. */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<AdminUserSummaryResponse> list(@RequestParam(required = false) String q,
                                                       @RequestParam(required = false) UserPlan plan,
                                                       @RequestParam(required = false) AdminUserStatusFilter status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return userService.list(q, plan, status, page, size);
    }

    @GetMapping("/{id}")
    public AdminUserDetailResponse detail(@PathVariable UUID id) {
        return userService.detail(id);
    }

    @PutMapping("/{id}/plan")
    public AdminUserDetailResponse changePlan(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable UUID id,
                                              @Valid @RequestBody ChangePlanRequest request, HttpServletRequest http) {
        return userService.changePlan(admin.id(), id, request, ClientContext.from(http));
    }

    @PostMapping("/{id}/block")
    public AdminUserDetailResponse block(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable UUID id,
                                         @Valid @RequestBody BlockUserRequest request, HttpServletRequest http) {
        return userService.block(admin.id(), id, request, ClientContext.from(http));
    }

    @PostMapping("/{id}/unblock")
    public AdminUserDetailResponse unblock(@AuthenticationPrincipal AdminPrincipal admin, @PathVariable UUID id,
                                           HttpServletRequest http) {
        return userService.unblock(admin.id(), id, ClientContext.from(http));
    }
}
