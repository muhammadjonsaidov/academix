package uz.academixai.wellbeing.application.port.out;

import uz.academixai.wellbeing.domain.BehaviorAnalysis;

/** External AI boundary for behavioral signal candidates. */
public interface BehaviorAnalyzer {

  BehaviorAnalysis analyze(String activitySummary);
}
