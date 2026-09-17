package uz.academixai.notification.application.port.out;

/** Outbound port for the bot username the deep-link URL is built from. */
public interface TelegramBotIdentity {

  String botUsername();
}
