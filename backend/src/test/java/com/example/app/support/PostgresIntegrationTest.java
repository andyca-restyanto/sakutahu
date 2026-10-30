// filepath: /backend/src/test/java/com/example/app/support/PostgresIntegrationTest.java
package com.example.app.support;

import com.example.app.modules.telegram.service.TelegramBotClient;
import com.example.app.shared.mail.MailService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

/**
 * Basis integration test: satu PostgreSQL asli (embedded, tanpa Docker) dan satu Spring context
 * yang dipakai bersama oleh semua subclass. Email di-mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTest {

    private static final EmbeddedPostgres PG = start();

    @MockitoBean
    protected MailService mailService;

    /** Tidak ada panggilan keluar ke api.telegram.org saat test. */
    @MockitoBean
    protected TelegramBotClient telegramBotClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("app.telegram.webhook-secret", () -> "test-secret");
        // Dipatok agar test tidak terpengaruh isi backend/.env.local developer
        registry.add("app.telegram.bot-username", () -> "SakuTahuBot");
        registry.add("app.frontend-url", () -> "http://localhost:5173");
        // OWNER tidak dibuat otomatis saat test (test membuat admin sendiri)
        registry.add("app.admin.bootstrap.password", () -> "");
    }

    private static EmbeddedPostgres start() {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    pg.close();
                } catch (IOException ignored) {
                    // proses test sudah selesai
                }
            }));
            return pg;
        } catch (IOException e) {
            throw new IllegalStateException("Gagal menjalankan embedded PostgreSQL", e);
        }
    }
}
