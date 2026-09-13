package uz.academixai.wellbeing.domain;

import java.util.List;

/** Provider-neutral result of analyzing a student's non-academic activity metadata. */
public record BehaviorAnalysis(List<SignalCandidate> signals, boolean manipulationSuspected) {}
