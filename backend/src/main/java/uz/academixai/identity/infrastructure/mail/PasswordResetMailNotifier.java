package uz.academixai.identity.infrastructure.mail;

import org.springframework.stereotype.Component;
import uz.academixai.identity.application.port.out.PasswordResetNotifier;
import uz.academixai.infrastructure.mail.PasswordResetMailSender;

/** Adapter for {@link PasswordResetNotifier} over the legacy SMTP sender. */
@Component
public class PasswordResetMailNotifier implements PasswordResetNotifier {

  private final PasswordResetMailSender sender;

  public PasswordResetMailNotifier(PasswordResetMailSender sender) {
    this.sender = sender;
  }

  @Override
  public void sendResetLink(String email, String resetUrl) {
    sender.sendResetLink(email, resetUrl);
  }
}
