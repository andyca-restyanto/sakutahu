// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminAuthResult.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.dto.AdminAuthResponse;

/** Body response + refresh token mentah (hanya untuk dijadikan cookie oleh controller). */
public record AdminAuthResult(AdminAuthResponse response, String refreshToken) {}
