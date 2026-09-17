package uz.academixai.notification.infrastructure.telegram;

import org.springframework.stereotype.Component;
import uz.academixai.infrastructure.telegram.TelegramProperties;
import uz.academixai.notification.application.port.out.TelegramBotIdentity;

/** Adapter exposing the configured bot username to the application layer. */
@Component
public class TelegramBotIdentityAdapter implements TelegramBotIdentity {

  private final TelegramProperties properties;

  public TelegramBotIdentityAdapter(TelegramProperties properties) {
    this.properties = properties;
  }

  @Override
  public String botUsername() {
    return properties.botUsername();
  }
}
