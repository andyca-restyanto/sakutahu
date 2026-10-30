// filepath: /backend/src/main/java/com/example/app/modules/admin/dto/AdminAuthResponse.java
package com.example.app.modules.admin.dto;

public record AdminAuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AdminResponse admin
) {}
