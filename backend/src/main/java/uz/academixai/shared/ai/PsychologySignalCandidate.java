package uz.academixai.shared.ai;

/** A possible psychology signal returned by an AI provider. */
public record PsychologySignalCandidate(
    String type, String severity, double confidence, String evidence) {}
