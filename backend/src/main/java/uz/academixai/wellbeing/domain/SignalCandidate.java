package uz.academixai.wellbeing.domain;

/**
 * Untrusted candidate returned by a behavioral-analysis provider; values are validated by the use
 * case.
 */
public record SignalCandidate(String type, String severity, String evidence) {}
