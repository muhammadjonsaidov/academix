package uz.academixai.interfaces.web.onboarding;

/** Lets the public UI decide whether to show the one-time setup call-to-action. */
public record InitialSetupStatusResponse(boolean available) {}
