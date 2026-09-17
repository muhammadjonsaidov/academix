package uz.academixai.interfaces.web.onboarding;

/** Public payload accepted only while an installation has no accounts. */
public record InitialSetupRequest(
    String firstName,
    String lastName,
    String phone,
    String email,
    String password,
    String schoolName,
    String region,
    String district,
    String address) {}
