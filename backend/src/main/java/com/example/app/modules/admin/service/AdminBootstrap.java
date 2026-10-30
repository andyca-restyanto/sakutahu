// filepath: /backend/src/main/java/com/example/app/modules/admin/service/AdminBootstrap.java
package com.example.app.modules.admin.service;

import com.example.app.modules.admin.entity.AdminRole;
import com.example.app.modules.admin.entity.AdminUser;
import com.example.app.modules.admin.repository.AdminUserRepository;
import com.example.app.shared.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Membuat OWNER pertama saat aplikasi start, HANYA jika belum ada admin sama sekali.
 * Email & password awal dari env (ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD); password wajib diganti
 * saat login pertama. Setelah OWNER ada, env ini tidak dipakai lagi dan boleh dihapus.
 * Tidak ada form pendaftaran admin publik.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties props;
    private final TransactionTemplate tx;

    public AdminBootstrap(AdminUserRepository repository, PasswordEncoder passwordEncoder, AdminProperties props,
                          TransactionTemplate tx) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        AdminProperties.Bootstrap b = props.bootstrap();
        if (b == null || b.password() == null || b.password().isBlank() || b.email() == null || b.email().isBlank()) {
            log.warn("Belum ada admin. Isi ADMIN_BOOTSTRAP_EMAIL & ADMIN_BOOTSTRAP_PASSWORD lalu restart untuk membuat OWNER.");
            return;
        }
        if (b.password().length() < 12) {
            log.error("ADMIN_BOOTSTRAP_PASSWORD minimal 12 karakter. OWNER tidak dibuat.");
            return;
        }
        String email = AdminMapper.normalizeEmail(b.email());
        String name = b.fullName() == null || b.fullName().isBlank() ? "Owner SakuTahu" : b.fullName().trim();
        try {
            tx.executeWithoutResult(status -> {
                if (repository.count() == 0) {
                    repository.save(new AdminUser(email, name, passwordEncoder.encode(b.password()),
                            AdminRole.OWNER, true, null));
                    log.info("OWNER admin dibuat untuk {}. Login di /admin/login dan ganti password awal.",
                            email.replaceAll("(^.{2}).*(@.*$)", "$1***$2"));
                }
            });
        } catch (DataIntegrityViolationException e) {
            // Instance lain membuat OWNER bersamaan; unique index menjamin hanya satu
            log.info("OWNER admin sudah dibuat oleh instance lain");
        }
    }
}
