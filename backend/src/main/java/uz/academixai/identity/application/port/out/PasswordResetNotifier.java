package uz.academixai.identity.application.port.out;

/**
 * Outbound port for delivering the reset link.
 *
 * <p>Delivery failure must not change the response: the caller answers the same generic success
 * either way so the endpoint cannot be used to enumerate accounts (see PasswordResetService).
 */
public interface PasswordResetNotifier {

  void sendResetLink(String email, String resetUrl);
}
