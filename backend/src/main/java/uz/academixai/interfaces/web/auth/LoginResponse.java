package uz.academixai.interfaces.web.auth;

public record LoginResponse(String accessToken, String refreshToken, UserSummary user) {}
