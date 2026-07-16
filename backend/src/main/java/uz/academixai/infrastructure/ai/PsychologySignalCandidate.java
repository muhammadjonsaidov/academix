package uz.academixai.infrastructure.ai;

/** One entry of the Qwen psychology-analysis response's {@code signals} array (TZ §3.3). */
public record PsychologySignalCandidate(
    String type, String severity, double confidence, String evidence) {}
