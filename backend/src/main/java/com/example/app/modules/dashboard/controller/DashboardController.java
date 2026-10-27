// filepath: /backend/src/main/java/com/example/app/modules/dashboard/controller/DashboardController.java
package com.example.app.modules.dashboard.controller;

import com.example.app.modules.dashboard.dto.DashboardResponse;
import com.example.app.modules.dashboard.service.DashboardService;
import com.example.app.shared.security.UserPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** Tanpa from/to: bulan berjalan menurut zona waktu user. */
    @GetMapping
    public DashboardResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.get(principal.id(), from, to);
    }
}
