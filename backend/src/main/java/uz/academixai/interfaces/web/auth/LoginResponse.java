package uz.academixai.interfaces.web.auth;

/** Refresh credentials are HttpOnly cookies and must never be exposed to browser JavaScript. */
public record LoginResponse(String accessToken, UserSummary user) {}
