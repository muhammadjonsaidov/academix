package uz.academixai.notification.adapter.in.web;

import uz.academixai.notification.application.TelegramLinkService.LinkTokenResult;

/** academix_tz.md §2.7 — POST /notifications/telegram/link-token, exact shape. */
public record LinkTokenResponse(String linkUrl, long expiresInSeconds) {

  public static LinkTokenResponse from(LinkTokenResult result) {
    return new LinkTokenResponse(result.linkUrl(), result.expiresInSeconds());
  }
}
