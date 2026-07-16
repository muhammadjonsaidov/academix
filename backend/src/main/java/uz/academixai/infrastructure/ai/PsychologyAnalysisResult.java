package uz.academixai.infrastructure.ai;

import java.util.List;

/** Qwen psychology-analysis response shape (TZ §3.3, exact). */
public record PsychologyAnalysisResult(
    List<PsychologySignalCandidate> signals, boolean isManipulationSuspected) {}
