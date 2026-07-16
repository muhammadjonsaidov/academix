package uz.academixai.domain;

/** backend_tdd.md §6.2 {@code verifyHandwritingProfile} return shape. */
public record HandwritingCheckResult(
    float matchScore, boolean profileWasReliable, PlagiarismType type) {}
