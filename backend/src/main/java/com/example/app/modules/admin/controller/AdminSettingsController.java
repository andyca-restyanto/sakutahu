// filepath: /backend/src/main/java/com/example/app/modules/admin/controller/AdminSettingsController.java
package com.example.app.modules.admin.controller;

import com.example.app.modules.admin.dto.ReceiptScanSettingsResponse;
import com.example.app.modules.admin.dto.UpdateReceiptScanSettingsRequest;
import com.example.app.modules.admin.service.AdminSettingsService;
import com.example.app.shared.security.AdminPrincipal;
import com.example.app.shared.util.ClientContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Menu "Kuota Struk". */
@RestController
@RequestMapping("/api/v1/admin/settings/receipt-scan")
public class AdminSettingsController {

    private final AdminSettingsService settingsService;

    public AdminSettingsController(AdminSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ReceiptScanSettingsResponse get() {
        return settingsService.receiptScan();
    }

    @PutMapping
    public ReceiptScanSettingsResponse update(@AuthenticationPrincipal AdminPrincipal admin,
                                              @Valid @RequestBody UpdateReceiptScanSettingsRequest request,
                                              HttpServletRequest http) {
        return settingsService.updateReceiptScan(admin.id(), request, ClientContext.from(http));
    }
}
