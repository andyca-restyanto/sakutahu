// filepath: /backend/src/main/java/com/example/app/shared/mail/MailService.java
package com.example.app.shared.mail;

import com.example.app.shared.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final AppProperties props;

    public MailService(JavaMailSender mailSender, ITemplateEngine templateEngine, AppProperties props) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.props = props;
    }

    /** Dipanggil secara asinkron oleh listener event (lihat VerificationEmailListener). */
    public void sendVerificationEmail(String to, String fullName, String verifyLink, Duration validFor) {
        Context ctx = new Context();
        ctx.setVariable("fullName", fullName);
        ctx.setVariable("verifyLink", verifyLink);
        ctx.setVariable("validHours", validFor.toHours());
        String html = templateEngine.process("mail/verify-email", ctx);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(parseFrom(props.mailFrom()));
            helper.setTo(to);
            helper.setSubject("Verifikasi email akun SakuTahu kamu");
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email verifikasi terkirim ke {}", maskEmail(to));
        } catch (MailAuthenticationException e) {
            log.error("Login SMTP ditolak saat mengirim ke {}. Untuk Gmail: pastikan MAIL_USERNAME adalah alamat Gmail "
                    + "lengkap, MAIL_PASSWORD adalah App Password 16 karakter (bukan password akun), dan "
                    + "verifikasi 2 langkah aktif.", maskEmail(to), e);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            log.error("Gagal mengirim email verifikasi ke {}", maskEmail(to), e);
        }
    }

    /** "SakuTahu <akun@gmail.com>" → InternetAddress dengan nama tampilan ter-encode UTF-8. */
    static InternetAddress parseFrom(String from) throws AddressException, UnsupportedEncodingException {
        InternetAddress parsed = new InternetAddress(from, true);
        if (parsed.getPersonal() != null) {
            parsed.setPersonal(parsed.getPersonal(), StandardCharsets.UTF_8.name());
        }
        return parsed;
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
