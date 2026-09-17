package uz.academixai.application.port.out.ai;

import java.util.List;

/** Provider output for a psychology analysis. */
public record PsychologyAnalysisResult(
    List<PsychologySignalCandidate> signals, boolean isManipulationSuspected) {}
