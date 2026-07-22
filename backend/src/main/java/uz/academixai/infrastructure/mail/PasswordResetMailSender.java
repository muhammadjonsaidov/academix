package uz.academixai.infrastructure.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Gmail SMTP send, wrapping {@link JavaMailSender} (Boot's own auto-configured bean from {@code
 * spring.mail.*} in application.yml). A failed/unconfigured send never throws — same
 * graceful-degradation principle this codebase already applies to Qwen/Vision/Telegram: no real
 * {@code GMAIL_USERNAME}/{@code GMAIL_APP_PASSWORD} exists in this dev environment, and {@code
 * PasswordResetService.forgotPassword} always returns the same generic success response regardless
 * (anti-enumeration), so a send failure must never surface differently to the caller than success.
 */
@Component
public class PasswordResetMailSender {

  private static final Logger log = LoggerFactory.getLogger(PasswordResetMailSender.class);

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username:}")
  private String fromAddress;

  public PasswordResetMailSender(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendResetLink(String toEmail, String resetUrl) {
    if (fromAddress == null || fromAddress.isBlank()) {
      log.warn("Password reset email skipped — no GMAIL_USERNAME/GMAIL_APP_PASSWORD configured.");
      return;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(toEmail);
      message.setSubject("AcademiX — parolni tiklash");
      message.setText(
          "Parolingizni tiklash uchun quyidagi havolani oching (30 daqiqa amal qiladi):\n\n"
              + resetUrl
              + "\n\nAgar bu so'rovni siz yubormagan bo'lsangiz, ushbu xabarni e'tiborsiz qoldiring.");
      mailSender.send(message);
    } catch (Exception e) {
      log.warn("Password reset email failed to send to {}", toEmail, e);
    }
  }
}
