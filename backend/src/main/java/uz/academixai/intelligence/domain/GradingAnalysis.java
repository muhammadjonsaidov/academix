package uz.academixai.intelligence.domain;

import java.util.List;
import uz.academixai.domain.CriteriaScore;
import uz.academixai.domain.StepAnalysis;

/** Provider-neutral result of the combined grading and plagiarism analysis. */
public record GradingAnalysis(
    List<CriteriaScore> criteriaScores,
    String feedback,
    List<StepAnalysis> stepAnalyses,
    double plagiarismScore,
    String plagiarismType) {}
