// filepath: /backend/src/main/java/com/example/app/modules/receipt/service/ReceiptProperties.java
package com.example.app.modules.receipt.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Konfigurasi pembaca struk (application*.yml → app.receipt.*). */
@ConfigurationProperties(prefix = "app.receipt")
public record ReceiptProperties(
        String provider,
        String apiKey,
        String model,
        String apiBaseUrl,
        Integer maxFileMb,
        Integer timeoutSeconds,
        /** Opsional. Wajib bila API key tidak terikat ke satu workspace (format wrkspc_...). */
        String workspaceId
) {
    public long maxFileBytes() {
        return (maxFileMb == null ? 7L : maxFileMb) * 1024L * 1024L;
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
